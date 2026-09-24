package application;

public class Customer {
    private int id;
    private String firstName;
    private String email;
    private String address;
    private String phone;
    private String membershipType;

    public Customer(int id, String firstName, String email, String address, String phone, String membershipType) {
        this.id = id;
        this.firstName = firstName;
        this.email = email;
        this.address = address;
        this.phone = phone;
        this.membershipType = membershipType;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType;}
}