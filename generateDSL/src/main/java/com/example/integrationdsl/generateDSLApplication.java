package com.example.integrationdsl;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;


@SpringBootApplication
public class generateDSLApplication {


	public static void main(String[] args) {

		/*try {
			CodeGeneration codeGeneration = new CodeGeneration();
			codeGeneration.generate();
		} catch (Exception e){
			e.printStackTrace();
		}*/

		new SpringApplicationBuilder(generateDSLApplication.class).web(WebApplicationType.NONE).run(args);

	}

}
