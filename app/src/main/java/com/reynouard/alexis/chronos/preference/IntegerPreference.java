package com.reynouard.alexis.chronos.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class IntegerPreference extends EditTextPreference {

    private int mDefaultValue = 0;

    public IntegerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public IntegerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntegerPreference(Context context) {
        super(context);
    }

    @Deprecated
    @Override
    public String getText() {
        return super.getText();
    }

    @Deprecated
    @Override
    public void setText(String text) {
        try {
            setInt(Integer.valueOf(text));
        }
        catch (NumberFormatException e) {
            setInt(mDefaultValue);
        }
    }

    public Integer getInt() {
        try {
            return Integer.valueOf(super.getText());
        }
        catch (NumberFormatException e) {
            return mDefaultValue;
        }
    }

    public void setInt(Integer integer) {
        super.setText(String.valueOf(integer));
    }

    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        super.onAddEditTextToDialogView(dialogView, editText);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        mDefaultValue = a.getInt(index, 0);
        return String.valueOf(mDefaultValue);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int defaultValue;
        try {
            defaultValue = Integer.valueOf(defaultReturnValue);
        }
        catch (NumberFormatException e) {
            defaultValue = mDefaultValue;
        }
        return String.valueOf(getPersistedInt(defaultValue));
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.valueOf(value));
    }
}
