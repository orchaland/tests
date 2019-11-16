package com.example.integrationdsl;

import javax.persistence.*;
import java.util.Date;


/**
 * The JPA Entity for the Student class
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 *
 * @since 2.2
 *
 */
@Entity(name = "Student")
@Table(name = "Student")
@NamedQueries({
        @NamedQuery(name = "selectAllStudents", query = "select s from Student s"),
        @NamedQuery(name = "selectStudent", query = "select s from Student s where s.lastName = 'Last One'"),
        @NamedQuery(name = "updateStudent",
                query = "update Student s set s.lastName = :lastName, s.lastUpdated = :lastUpdated where s.rollNumber "
                        + "in (select max(a.rollNumber) from Student a)")
})
@NamedNativeQuery(resultClass = StudentDomain.class, name = "updateStudentNativeQuery",
        query = "update Student s set s.lastName = :lastName, lastUpdated = :lastUpdated where s.rollNumber "
                + "in (select max(a.rollNumber) from Student a)")
public class StudentDomain {

    @Id
    @Column(name = "rollNumber")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long rollNumber;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "lastName")
    private String lastName;

    @Column(name = "gender")
    private String gender;

    @Column(name = "dateOfBirth")
    @Temporal(TemporalType.DATE)
    private Date dateOfBirth;

    @Column(name = "lastUpdated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated;

    public Long getRollNumber() {
        return this.rollNumber;
    }

    public void setRollNumber(Long rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Gender getGender() {
        return Gender.getGenderFromIdentifier(this.gender);
    }

    public void setGender(Gender gender) {
        this.gender = gender.getIdentifier();
    }

    public Date getDateOfBirth() {
        return this.dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }


    public Date getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    //Convenience methods for chaining method calls

    public StudentDomain withRollNumber(Long rollNumber) {
        setRollNumber(rollNumber);
        return this;
    }

    public StudentDomain withFirstName(String firstName) {
        setFirstName(firstName);
        return this;
    }

    public StudentDomain withLastName(String lastName) {
        setLastName(lastName);
        return this;
    }

    public StudentDomain withGender(Gender gender) {
        setGender(gender);
        return this;
    }

    public StudentDomain withDateOfBirth(Date dateOfBirth) {
        setDateOfBirth(dateOfBirth);
        return this;
    }

    public StudentDomain withLastUpdated(Date lastUpdated) {
        setLastUpdated(lastUpdated);
        return this;
    }

}