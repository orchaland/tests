package com.example.jpa


import javax.persistence.*


@Entity(name = "Student")
class StudentDomain {
    @Id
    @Column(name = "rollNumber")
    @GeneratedValue(strategy = GenerationType.AUTO)
    var rollNumber: Long? = null

    @Column(name = "firstName")
    var firstName: String? = null

    @Column(name = "age")
    var age = 0
    constructor(firstName: String?, age: Int) {
        this.firstName = firstName
        this.age = age
    }

    constructor(firstName: String?, age: Int, rollNumber: Long) {
        this.firstName = firstName
        this.age = age
        this.rollNumber = rollNumber
    }

    override fun toString(): String {
        return "StudentDomain{" +
                "rollNumber=" + rollNumber +
                ", firstName='" + firstName + '\'' +
                ", age=" + age +
                '}'
    }
}