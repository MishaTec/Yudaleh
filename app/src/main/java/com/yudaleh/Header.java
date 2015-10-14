package com.yudaleh;

/**
 * Represents a single header item in the expandable list of debts.
 */
class Header {
    private String ownerPhone;
    private String ownerName;
    private double totalMoney;
    private int color;

    Header(String ownerPhone, String ownerName) {
        this.ownerPhone = ownerPhone;
        this.ownerName = ownerName;
    }

    Header(String ownerPhone, String ownerName, double totalMoney) {
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
        if (!(o instanceof Header)) {
            return false;
        }
        Header other = (Header) o;
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
