package com.yudaleh;

/**
 * Created by Michael on 04/10/2015.
 */
class Contact {
    private String phone;
    private String name;
    private double totalMoney;

    Contact(String phone, String name) {
        this.phone = phone;
        this.name = name;
    }

    Contact(String phone, String name, double totalMoney) {
        this.phone = phone;
        this.name = name;
        this.totalMoney = totalMoney;
    }

    String getPhone() {
        return phone;
    }

    void setPhone(String phone) {
        this.phone = phone;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
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
        return EqualsUtil.areEqual(phone, other.phone)
                && EqualsUtil.areEqual(name, other.name)
                && EqualsUtil.areEqual(totalMoney, other.totalMoney);
    }

    String getMapKey() {
        return phone != null ? phone : name;
    }
}
