package com.taxibooking.model; //User

/** Base account. Stored in users.txt as: id|fullName|email|phone|passwordHash|address|role|status */
public class User {
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String password;   // BCrypt hash
    private String address;
    private String role;
    private String status = ACTIVE;

    public User() { }

    public String toFileLine() {
        return String.join("|", clean(userId), clean(fullName), clean(email), clean(phone),
                clean(password), clean(address), clean(role), clean(status));
    }

    /** The file uses '|' as a separator, so it must never appear inside a value. */
    public static String clean(String s) {
        return s == null ? "" : s.replace("|", " ").replace("\r", " ").replace("\n", " ").trim();
    }

    public boolean isActive() { return ACTIVE.equals(status); }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

