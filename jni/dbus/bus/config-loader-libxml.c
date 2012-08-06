/* -*- mode: C; c-file-style: "gnu"; indent-tabs-mode: nil; -*- */
/* config-loader-libxml.c  libxml2 XML loader
 *
 * Copyright (C) 2003 Red Hat, Inc.
 *
 * Licensed under the Academic Free License version 2.1
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

#include <config.h>
#include "config-parser.h"
#include <dbus/dbus-internals.h>
#include <libxml/xmlreader.h>
#include <libxml/parser.h>
#include <libxml/globals.h>
#include <libxml/xmlmemory.h>
#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif
#include <string.h>

/* About the error handling: 
 *  - setup a "structured" error handler that catches structural
 *    errors and some oom errors 
 *  - assume that a libxml function returning an error code means
 *    out-of-memory
 */
#define _DBUS_MAYBE_SET_OOM(e) (dbus_error_is_set(e) ? (void)0 : _DBUS_SET_OOM(e))


static dbus_bool_t
xml_text_start_element (BusConfigParser   *parser,
			xmlTextReader     *reader,
			DBusError         *error)
{
  const char *name;
  int n_attributes;
  const char **attribute_names, **attribute_values;
  dbus_bool_t ret;
  int i, status, is_empty;

  _DBUS_ASSERT_ERROR_IS_CLEAR (error);

  ret = FALSE;
  attribute_names = NULL;
  attribute_values = NULL;

  name = xmlTextReaderConstName (reader);
  n_attributes = xmlTextReaderAttributeCount (reader);
  is_empty = xmlTextReaderIsEmptyElement (reader);

  if (name == NULL || n_attributes < 0 || is_empty == -1)
    {
      _DBUS_MAYBE_SET_OOM (error);
      goto out;
    }

  attribute_names = dbus_new0 (const char *, n_attributes + 1);
  attribute_values = dbus_new0 (const char *, n_attributes + 1);
  if (attribute_names == NULL || attribute_values == NULL)
    {
      _DBUS_SET_OOM (error);
      goto out;
    }
  i = 0;
  while ((status = xmlTextReaderMoveToNextAttribute (reader)) == 1)
    {
      _dbus_assert (i < n_attributes);
      attribute_names[i] = xmlTextReaderConstName (reader);
      attribute_values[i] = xmlTextReaderConstValue (reader);
      if (attribute_names[i] == NULL || attribute_values[i] == NULL)
	{ 
          _DBUS_MAYBE_SET_OOM (error);
	  goto out;
	}
      i++;
    }
  if (status == -1)
    {
      _DBUS_MAYBE_SET_OOM (error);
      goto out;
    }
  _dbus_assert (i == n_attributes);

  ret = bus_config_parser_start_element (parser, name,
					 attribute_names, attribute_values,
					 error);
  if (ret && is_empty == 1)
    ret = bus_config_parser_end_element (parser, name, error);

 out:
  dbus_free (attribute_names);
  dbus_free (attribute_values);

  return ret;
}

static void xml_shut_up (void *ctx, const char *msg, ...)
{
    return;
}

static void
xml_text_reader_error (void *arg, xmlErrorPtr xml_error)
{
  DBusError *error = arg;

#if 0
  _dbus_verbose ("XML_ERROR level=%d, domain=%d, code=%d, msg=%s\n",
                 xml_error->level, xml_error->domain,
                 xml_error->code, xml_error->message);
#endif

  if (!dbus_error_is_set (error))
    {
      if (xml_error->code == XML_ERR_NO_MEMORY)
        _DBUS_SET_OOM (error);
      else if (xml_error->level == XML_ERR_ERROR ||
               xml_error->level == XML_ERR_FATAL)
        dbus_set_error (error, DBUS_ERROR_FAILED,
                        "Error loading config file: '%s'",
                        xml_error->message);
    }
}


BusConfigParser*
bus_config_load (const DBusString      *file,
                 dbus_bool_t            is_toplevel,
                 const BusConfigParser *parent,
                 DBusError             *error)

{
  xmlTextReader *reader;
  BusConfigParser *parser;
  DBusString dirname, data;
  DBusError tmp_error;
  int ret;
  
  _DBUS_ASSERT_ERROR_IS_CLEAR (error);
  
  parser = NULL;
  reader = NULL;

  if (!_dbus_string_init (&dirname))
    {
      _DBUS_SET_OOM (error);
      return NULL;
    }

  if (!_dbus_string_init (&data))
    {
      _DBUS_SET_OOM (error);
      _dbus_string_free (&dirname);
      return NULL;
    }

  if (is_toplevel)
    {
      /* xmlMemSetup only fails if one of the functions is NULL */
      xmlMemSetup (dbus_free,
                   dbus_malloc,
                   dbus_realloc,
                   _dbus_strdup);
      xmlInitParser ();
      xmlSetGenericErrorFunc (NULL, xml_shut_up);
    }

  if (!_dbus_string_get_dirname (file, &dirname))
    {
      _DBUS_SET_OOM (error);
      goto failed;
    }
  
  parser = bus_config_parser_new (&dirname, is_toplevel, parent);
  if (parser == NULL)
    {
      _DBUS_SET_OOM (error);
      goto failed;
    }
  
  if (!_dbus_file_get_contents (&data, file, error))
    goto failed;

  reader = xmlReaderForMemory (_dbus_string_get_const_data (&data), 
                               _dbus_string_get_length (&data),
			       NULL, NULL, 0);
  if (reader == NULL)
    {
      _DBUS_SET_OOM (error);
      goto failed;
    }

  xmlTextReaderSetParserProp (reader, XML_PARSER_SUBST_ENTITIES, 1);

  dbus_error_init (&tmp_error);
  xmlTextReaderSetStructuredErrorHandler (reader, xml_text_reader_error, &tmp_error);

  while ((ret = xmlTextReaderRead (reader)) == 1)
    {
      int type;
      
      if (dbus_error_is_set (&tmp_error))
        goto reader_out;

      type = xmlTextReaderNodeType (reader);
      if (type == -1)
        {
          _DBUS_MAYBE_SET_OOM (&tmp_error);
          goto reader_out;
        }

      switch ((xmlReaderTypes) type) {
      case XML_READER_TYPE_ELEMENT:
	xml_text_start_element (parser, reader, &tmp_error);
	break;

      case XML_READER_TYPE_TEXT:
      case XML_READER_TYPE_CDATA:
	{
	  DBusString content;
	  const char *value;
	  value = xmlTextReaderConstValue (reader);
	  if (value != NULL)
	    {
	      _dbus_string_init_const (&content, value);
	      bus_config_parser_content (parser, &content, &tmp_error);
	    }
          else
            _DBUS_MAYBE_SET_OOM (&tmp_error);
	  break;
	}

      case XML_READER_TYPE_DOCUMENT_TYPE:
	{
	  const char *name;
	  name = xmlTextReaderConstName (reader);
	  if (name != NULL)
	    bus_config_parser_check_doctype (parser, name, &tmp_error);
          else
            _DBUS_MAYBE_SET_OOM (&tmp_error);
	  break;
	}

      case XML_READER_TYPE_END_ELEMENT:
	{
	  const char *name;
	  name = xmlTextReaderConstName (reader);
	  if (name != NULL)
	    bus_config_parser_end_element (parser, name, &tmp_error);
          else
            _DBUS_MAYBE_SET_OOM (&tmp_error);
	  break;
	}

      case XML_READER_TYPE_DOCUMENT:
      case XML_READER_TYPE_DOCUMENT_FRAGMENT:
      case XML_READER_TYPE_PROCESSING_INSTRUCTION:
      case XML_READER_TYPE_COMMENT:
      case XML_READER_TYPE_ENTITY:
      case XML_READER_TYPE_NOTATION:
      case XML_READER_TYPE_WHITESPACE:
      case XML_READER_TYPE_SIGNIFICANT_WHITESPACE:
      case XML_READER_TYPE_END_ENTITY:
      case XML_READER_TYPE_XML_DECLARATION:
	/* nothing to do, just read on */
	break;

      case XML_READER_TYPE_NONE:
      case XML_READER_TYPE_ATTRIBUTE:
      case XML_READER_TYPE_ENTITY_REFERENCE:
	_dbus_assert_not_reached ("unexpected nodes in XML");
      }

      if (dbus_error_is_set (&tmp_error))
        goto reader_out;
    }

  if (ret == -1)
    _DBUS_MAYBE_SET_OOM (&tmp_error);

 reader_out:
  xmlFreeTextReader (reader);
  reader = NULL;
  if (dbus_error_is_set (&tmp_error))
    {
      dbus_move_error (&tmp_error, error);
      goto failed;
    }
  
  if (!bus_config_parser_finished (parser, error))
    goto failed;
  _dbus_string_free (&dirname);
  _dbus_string_free (&data);
  if (is_toplevel)
    xmlCleanupParser();
  _DBUS_ASSERT_ERROR_IS_CLEAR (error);
  return parser;
  
 failed:
  _DBUS_ASSERT_ERROR_IS_SET (error);
  _dbus_string_free (&dirname);
  _dbus_string_free (&data);
  if (is_toplevel)
    xmlCleanupParser();
  if (parser)
    bus_config_parser_unref (parser);
  _dbus_assert (reader == NULL); /* must go to reader_out first */
  return NULL;
}
