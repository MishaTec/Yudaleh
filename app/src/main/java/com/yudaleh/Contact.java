package com.yudaleh;

import android.graphics.Color;

/**
 * Created by Michael on 04/10/2015.
 */
class Contact {
    private String ownerPhone;
    private String ownerName;
    private double totalMoney;
    private int color;

    Contact(String ownerPhone, String ownerName) {
        this.ownerPhone = ownerPhone;
        this.ownerName = ownerName;
    }

    Contact(String ownerPhone, String ownerName, double totalMoney) {
        this.ownerPhone = ownerPhone;
        this.ownerName = ownerName;
        this.totalMoney = totalMoney;
    }

    String getOwnerPhone() {
        return ownerPhone;
    }

    void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    String getOwnerName() {
        return ownerName;
    }

    void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    double getTotalMoney() {
        return totalMoney;
    }

    void setTotalMoney(double totalMoney) {
        this.totalMoney = totalMoney;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Contact)) {
            return false;
        }
        Contact other = (Contact) o;
        return EqualsUtil.areEqual(ownerPhone, other.ownerPhone)
                && EqualsUtil.areEqual(ownerName, other.ownerName)
                && EqualsUtil.areEqual(totalMoney, other.totalMoney);
    }

    String getMapKey() {
        return ownerPhone != null ? ownerPhone : ownerName;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
