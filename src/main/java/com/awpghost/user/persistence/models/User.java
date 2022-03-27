package com.awpghost.user.persistence.models;


import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@Data
@Builder
@Document("user")
public class User {
    @Id
    private String id;

    @ArangoId
    private String arangoId;

    private String firstName;

    private String lastName;

    private String email;

    private String location; // Country/region code ISO 3166-2

    private String mobileNo;

    private String address1;

    private String address2;

    private String city;

    private String state;

    private String zip;

}
