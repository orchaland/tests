package orcha.lang.compiler.referenceimpl.springIntegration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean

class DatabaseConnection{

    @Value("\${spring-datasource-driverClassName}")
    lateinit var driver: String

    @Value("\${spring-datasource-url}")
    lateinit var url: String

    @Value("\${spring-datasource-username}")
    lateinit var login: String

    @Value("\${spring-datasource-password}")
    lateinit var password: String


    @Bean
    fun databaseConnection() : DatabaseConnection {
        val connection: DatabaseConnection = DatabaseConnection()
        if(connection.driver==null||connection.url==null||connection.login==null||
                connection.password==null)throw Exception("driver, url, login and password should not be null.Consider initialization in a property file.")
        return connection
    }
}