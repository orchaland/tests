package orchalang

import orcha.lang.compiler.referenceimpl.springIntegration.DatabaseConnection
import orcha.lang.configuration.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class OrchaCompilerConfiguration {

    @Bean
    fun studentDatabase(): EventHandler {
        val eventHandler = EventHandler("studentDatabase")
        val databaseAdapter = DatabaseAdapter()
        eventHandler.input= Input(databaseAdapter,"com.example.jpa.StudentDomain ")
        return eventHandler
    }

    @Bean
           fun databaseConnection() : DatabaseConnection {
               val connection: DatabaseConnection = DatabaseConnection()
               if(connection.driver==null||connection.url==null||connection.login==null||
               connection.password==null)throw Exception("driver, url, login and password should not be null.Consider initialization in a property file.")
     return connection
       }
    @Bean
    fun enrollStudent(): Application {
        val application = Application("enrollStudent", "Kotlin")
        val javaAdapter = JavaServiceAdapter("com.example.jpa.EnrollStudent", "enroll")
        application.input = Input(javaAdapter, "com.example.jpa.StudentDomain")
        application.output = Output(javaAdapter, "orcha.lang.compiler.OrchaProgram")
        return application
    }



    @Bean
    fun studentOutputDatabase(): EventHandler {
        val eventHandler = EventHandler("studentOutputDatabase")
        val databaseAdapter = DatabaseAdapter()
        eventHandler.output = Output(databaseAdapter, "com.example.jpa.StudentDomain")
        return eventHandler
    }
}