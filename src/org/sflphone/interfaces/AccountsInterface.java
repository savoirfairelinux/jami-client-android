package org.sflphone.interfaces;

import android.content.Intent;

public interface AccountsInterface {
    
    public void accountsChanged();

    public void accountStateChanged(Intent accountState);


}
