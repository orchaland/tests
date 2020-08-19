package com.example.mongodbReadWrite

import org.springframework.integration.annotation.Gateway

interface School {
    fun readDatabase(student: StudentDomain?)
    fun saveStudent(student: StudentDomain?)


}