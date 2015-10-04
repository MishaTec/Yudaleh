package com.yudaleh;

/**
 * Created by Michael on 04/10/2015.
 */
class Contact {
    private String phone;
    private String name;
    private double totalMoney;

    public Contact(String phone, String name, double totalMoney) {
        this.phone = phone;
        this.name = name;
        this.totalMoney = totalMoney;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(double totalMoney) {
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
}
