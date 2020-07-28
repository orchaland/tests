package orcha.lang.compiler.referenceimpl.springIntegration

import com.helger.jcodemodel.*
import com.helger.jcodemodel.writer.FileCodeWriter
import orcha.lang.compiler.IntegrationNode
import orcha.lang.compiler.OrchaMetadata
import orcha.lang.compiler.syntax.ComputeInstruction
import orcha.lang.compiler.syntax.WhenInstruction
import orcha.lang.configuration.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.Pollers
import java.io.File


class OutputCodeGenerationToSpringIntegrationJavaDSLImpl : OutputCodeGenerationToSpringIntegrationJavaDSL {
    var generatedClass : JDefinedClass ? = null
    val codeModel = JCodeModel()
    override fun orchaMetadata(orchaMetadata: OrchaMetadata) {
        val className = orchaMetadata.domainAsCapitalizedConcatainedString!!.decapitalize() + "." + orchaMetadata.titleAsCapitalizedConcatainedString + "Application"
        log.info("Generated class name: " + className)
        generatedClass = codeModel._class(JMod.PUBLIC, className , EClassType.CLASS)
        val jAnnotation: JAnnotationUse = generatedClass!!.annotate(SpringBootApplication::class.java)
        jAnnotation.paramArray("scanBasePackages", orchaMetadata.domain);
    }
    override fun inputAdapter(eventHandler: EventHandler, nextIntegrationNodes: List<IntegrationNode>) {
        val adapter = eventHandler.input!!.adapter
        when(adapter){
            is InputFileAdapter -> {
                val inputFileAdapter: InputFileAdapter = adapter as InputFileAdapter
                log.info("Generation of the output code for " + inputFileAdapter)

            }
        }
    }
    override fun outputAdapter(adapter: ConfigurableProperties) {
        when(adapter){
            is OutputFileAdapter -> {
                val outputFileAdapter: OutputFileAdapter = adapter as OutputFileAdapter
                log.info("Generation of the output code for " + outputFileAdapter)
            }
        }
    }
    override fun filter(expression: String) {
        log.info("Generation of a filter for the expression " + expression)
    }
    override fun serviceActivator(application: Application, nextIntegrationNodes: List<IntegrationNode>) {
        val adapter = application.input!!.adapter
        when(adapter){
            is JavaServiceAdapter -> {
                val javaServiceAdapter: JavaServiceAdapter = adapter as JavaServiceAdapter
                log.info("Generation of a service activator for " + javaServiceAdapter)

            }
        }
    }
    override fun aggregator(whenInstruction: WhenInstruction, nextIntegrationNodes: List<IntegrationNode>) {
        log.info("Generation of the output code for " + whenInstruction)

    }
    override fun export() {


    }




    companion object {
        private val log = LoggerFactory.getLogger(OutputCodeGenerationToSpringIntegrationJavaDSLImpl::class.java)
    }

}