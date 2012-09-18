package com.savoirfairelinux.sflphone.client;

public class CustomListEntry
{
    private String mTextAlpha;
    private String mTextBeta;

    public CustomListEntry(String textA, String textB)
    {
        mTextAlpha = textA;
        mTextBeta = textB;
    }

    public String getTextAlpha()
    {
        return mTextAlpha;
    }

    public String getTextBeta()
    {
        return mTextBeta;
    }
}
