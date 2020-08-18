package com.example.mongodbReadWrite

//import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
//class PopulateDatabase(val mongoOps: ReactiveMongoTemplate = ReactiveMongoTemplate(MongoClients.create(), "test")) {
class PopulateDatabase() {

    fun saveStudent(student: StudentDomain) {
       // mongoOps.insert(student)
    }

    @Throws(Exception::class)
    fun enrollStudent(student: StudentDomain): StudentDomain {
        print("enrollStudent receives: $student")
        student.rollNumber = 1
        println(" and returns: $student")
        return student
    }

    fun readDatabase(): Flux<StudentDomain>? {
        val query = Query()
       // return mongoOps!!.find(query, StudentDomain::class.java)
        return null
    }

}