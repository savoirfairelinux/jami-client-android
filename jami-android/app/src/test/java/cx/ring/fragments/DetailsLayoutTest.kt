package cx.ring.fragments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DetailsLayoutTest {
    @Test
    fun conversationDetailsContainJoinedButNotContactDatesOrIdentity() {
        val ids = elements("layout/frag_conversation_actions.xml").map { it.androidAttribute("id") }.toSet()
        assertTrue(ids.contains("@+id/conversation_joined_date"))
        assertFalse(ids.contains("@+id/contact_added_date"))
        assertFalse(ids.contains("@+id/contact_removed_date"))
        assertFalse(ids.contains("@+id/user_name"))
        assertFalse(ids.contains("@+id/identifier"))
        assertFalse(ids.contains("@+id/contact_details"))
    }

    @Test
    fun contactDetailsContainIdentityQrAndContactDatesButNotJoined() {
        val ids = elements("layout/frag_contact_details.xml").map { it.androidAttribute("id") }.toSet()
        for (id in listOf("contact_avatar", "contact_name", "registered_name", "identifier",
            "qr_code", "share_button", "contact_added_date", "contact_removed_date")) {
            assertTrue("Missing contact detail: $id", ids.contains("@+id/$id"))
        }
        assertFalse(ids.contains("@+id/conversation_joined_date"))
    }

    @Test
    fun conversationMenuHasNoContactDetailsAction() {
        val items = elements("menu/conversation_actions.xml")
            .filter { it.tagName == "item" }
            .associate { it.androidAttribute("id") to it.androidAttribute("title") }
        assertFalse(items.containsValue("@string/contact_details"))
        assertEquals("@string/conversation_details", items["@+id/conv_conversation_details"])
    }

    @Test
    fun contactLongPressMenusAppendDetailsWithoutChangingExistingActions() {
        val copy = "@string/conversation_action_copy_peer_number"
        val delete = "@string/delete_contact"
        val block = "@string/conversation_action_block_this"
        val details = "@string/contact_details"
        assertEquals(listOf(copy, "@string/conversation_action_history_clear", delete, block, details),
            resourceArray("conversation_actions"))
        assertEquals(listOf(copy, "@string/remove_conversation", delete, block, details),
            resourceArray("swarm_one_to_one_actions"))
        for (name in listOf("conversation_action_icons", "swarm_one_to_one_action_icons")) {
            val icons = resourceArray(name)
            assertEquals(5, icons.size)
            assertEquals("@drawable/outline_person_24", icons.last())
        }
        assertFalse(resourceArray("swarm_group_actions").contains(details))
    }

    private fun resourceArray(name: String): List<String> {
        val array = elements("values/arrays.xml").single { it.getAttribute("name") == name }
        val items = array.getElementsByTagName("item")
        return (0 until items.length).map { items.item(it).textContent.trim() }
    }

    private fun elements(resource: String): List<Element> {
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File("src/main/res", resource))
        val elements = document.getElementsByTagName("*")
        return (0 until elements.length).map { elements.item(it) as Element }
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS("http://schemas.android.com/apk/res/android", name)
}
