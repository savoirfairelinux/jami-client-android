/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package com.savoirfairelinux.sflphone.client;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.R.layout;
import com.savoirfairelinux.sflphone.R.menu;
import com.savoirfairelinux.sflphone.fragments.ContributeFragment;
import com.savoirfairelinux.sflphone.fragments.HelpGesturesFragment;
import com.savoirfairelinux.sflphone.fragments.LegalFragment;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This Activity holds some conex fragments not requiring a lot of interaction: HelpGesturesFragment, LegalFragment, ContributeFragment
 * @author lisional
 *
 */
public class ActivityHolder extends Activity {
    
    public interface args {
        int FRAG_GESTURES = 0;
        int FRAG_LEGAL = 1;
        int FRAG_CONTRIBUTE = 2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holder);
        
        
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        switch(getIntent().getIntExtra("ActivityHolder.args", -1)){
        case args.FRAG_GESTURES:
            ft.replace(R.id.frag_container, new HelpGesturesFragment());
            break;
        case args.FRAG_LEGAL:
            ft.replace(R.id.frag_container, new LegalFragment());
            break;
        case args.FRAG_CONTRIBUTE:
            ft.replace(R.id.frag_container, new ContributeFragment());
            break;
        }
        
        ft.commit();
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return true;
        }
    }

}
