package com.yudaleh;


import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@ParseClassName("Debt")
public class Debt extends ParseObject {

    static final String KEY_UUID = "uuid";
    static final String KEY_DATE_CREATED = "dateCreated";
    static final String KEY_IS_DRAFT = "isDraft";
//    static final String KEY_AUTHOR = "author";// REMOVE: 07/10/2015
    static final String KEY_AUTHOR_NAME = "authorName";
    static final String KEY_AUTHOR_PHONE = "authorPhone";
    static final String KEY_OTHER_UUID = "origUuid";
    static final String KEY_DUE_DATE = "dueDate";
    static final String KEY_DESCRIPTION = "description";
    static final String KEY_TITLE = "title";
    static final String KEY_CURRENCY_POS = "currencyPos";
    static final String KEY_MONEY_AMOUNT = "money";
    static final String KEY_OWNER_NAME = "ownerName";
    static final String KEY_STATUS = "status";
    static final String KEY_OWNER_PHONE = "ownerPhone";
    static final String KEY_TAB_TAG = "tabTag";

    static final int NON_MONEY_DEBT_CURRENCY = 0;

    static final String I_OWE_TAG = "iOwe";
    static final String OWE_ME_TAG = "oweMe";

    static final int STATUS_CREATED = 1;
    static final int STATUS_PENDING = 2;
    static final int STATUS_CONFIRMED = 3;
    static final int STATUS_RETURNED = 4;
    static final int STATUS_DELETED = 5;

    String getTabTag() {
        return getString(KEY_TAB_TAG);
    }

    void setTabTag(String tabTag) {
        if (tabTag != null) {
            put(KEY_TAB_TAG, tabTag);
        } else {
            remove(KEY_TAB_TAG);
        }
    }

    String getTitle() {
        return getString(KEY_TITLE);
    }

    void setTitle(String title) {
        if (title != null && title.length() > 0) {
            put(KEY_TITLE, title.trim());
        } else {
            remove(KEY_TITLE);
        }
    }

    int getCurrencyPos() {
        return getInt(KEY_CURRENCY_POS);
    }

    void setCurrencyPos(int currencyPos) {
        put(KEY_CURRENCY_POS, currencyPos);
    }

    double getMoneyAmount() {
        return getDouble(KEY_MONEY_AMOUNT);
    }

    void setMoneyAmount(double moneyAmount) {
        put(KEY_MONEY_AMOUNT, moneyAmount);
    }

    public void setMoneyAmountByTitle() {
        String title = getTitle();
        if (title == null || getCurrencyPos() == NON_MONEY_DEBT_CURRENCY) {
            setMoneyAmount(0);
        } else {
            try {
                setMoneyAmount(Double.valueOf(title.replaceAll("[^\\d.]+|\\.(?!\\d)", "")));
            } catch (NumberFormatException e) {
                setMoneyAmount(0);
            }
        }
    }

    String getOwnerName() {
        return getString(KEY_OWNER_NAME);
    }

    void setOwnerName(String ownerName) {
        if (ownerName != null && ownerName.length() > 0) {
            put(KEY_OWNER_NAME, ownerName.trim());
        } else {
            remove(KEY_OWNER_NAME);
        }
    }

    int getStatus() {
        return getInt(KEY_STATUS);
    }

    void setStatus(int status) {
        put(KEY_STATUS, status);
    }

    String getOwnerPhone() {
        return getString(KEY_OWNER_PHONE);
    }

    void setOwnerPhone(String ownerPhone, String userCountry) {
        if (ownerPhone != null && ownerPhone.length() > 0) {
            // Format phone number to E164 standard to use it as a unique identifier
            put(KEY_OWNER_PHONE, formatToE164(ownerPhone, userCountry).trim());
        } else {
            remove(KEY_OWNER_PHONE);
        }
    }

    private String formatToE164(String phone, String userCountry) {
        if (phone == null) {
            return null;
        }
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber numberProto = null;
        try {
            numberProto = phoneUtil.parse(phone, userCountry);
        } catch (NumberParseException e) {
            System.err.println("NumberParseException was thrown: " + e.toString());
        }
        String formatted = null;
        if (numberProto != null) {
            formatted = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
        }
        return (formatted != null ? formatted : phone.replaceAll("[^0-9+]+", ""));
    }


    String getDescription() {
        return getString(KEY_DESCRIPTION);
    }

    void setDescription(String description) {
        if (description != null && description.length() > 0) {
            put(KEY_DESCRIPTION, description.trim());
        } else {
            remove(KEY_DESCRIPTION);
        }
    }

    Date getDueDate() {
        return getDate(KEY_DUE_DATE);
    }

    void setDueDate(Date dueDate) {
        if (dueDate != null) {
            put(KEY_DUE_DATE, dueDate);
        } else {
            remove(KEY_DUE_DATE);
        }
    }

    Date getDateCreated() {
        return getDate(KEY_DATE_CREATED);
    }

    void setDateCreated(Date dateCreated) {
        if (dateCreated != null) {
            put(KEY_DATE_CREATED, dateCreated);
        } else {
            remove(KEY_DATE_CREATED);
        }
    }

/*    @Deprecated
    ParseUser getAuthor() {
        return getParseUser(KEY_AUTHOR);
    }

    void setAuthor(ParseUser currentUser) {
        if (currentUser != null) {
            put(KEY_AUTHOR, currentUser);
        } else {
            remove(KEY_AUTHOR);
        }
    }*/// REMOVE: 07/10/2015

    String getAuthorName() {
        return getString(KEY_AUTHOR_NAME);
    }

    void setAuthorName(String authorName) {
        if (authorName != null && authorName.length() > 0) {
            put(KEY_AUTHOR_NAME, authorName.trim());
        } else {
            remove(KEY_AUTHOR_NAME);
        }
    }

    String getAuthorPhone() {
        return getString(KEY_AUTHOR_PHONE);
    }

    void setAuthorPhone(String authorPhone) {
        if (authorPhone != null && authorPhone.length() > 0) {
            put(KEY_AUTHOR_PHONE, authorPhone.trim());
        } else {
            remove(KEY_AUTHOR_PHONE);
        }
    }

    String getOtherUuid() {
        return getString(KEY_OTHER_UUID);
    }

    void setOtherUuid(String otherUuid) {
        if (otherUuid != null && otherUuid.length() > 0) {
            put(KEY_OTHER_UUID, otherUuid.trim());
        } else {
            remove(KEY_OTHER_UUID);
        }
    }

    boolean isDraft() {
        return getBoolean(KEY_IS_DRAFT);
    }

    void setDraft(boolean isDraft) {
        put(KEY_IS_DRAFT, isDraft);
    }

    void setUuidString() {
        UUID uuid = UUID.randomUUID();
        put(KEY_UUID, uuid.toString());
    }

    String getUuidString() {
        return getString(KEY_UUID);
    }

    static ParseQuery<Debt> getQuery() {
        return ParseQuery.getQuery(Debt.class);
    }

    boolean equals(Debt other) {
        if (keySet().size() != other.keySet().size()) {
            return false;
        }
        for (String key : other.keySet()) {
            if (!other.get(key).equals(get(key))) {
                return false;
            }
        }
        return true;
    }

    Debt createClone() {
        Debt clone = new Debt();
        for (String key : keySet()) {
            clone.put(key, get(key));
        }
        return clone;
    }

    void copyFrom(Debt other){
        Set<String> otherKeys = other.keySet();

        for (String key : otherKeys) {
            put(key, other.get(key));
        }

        // Make sure there are no extra keys
        for (String key : keySet()) {
            if (!otherKeys.contains(key)) {
                remove(key);
            }
        }
    }

}
