package com.example.integrationdsl;

import com.helger.jcodemodel.*;

import java.io.File;

public class CodeGeneration {

    public void generate() throws Exception{

        JCodeModel codeModel = new JCodeModel();
        String className = "com.example.integrationdsl." + "RestWebService";
        JDefinedClass webService = codeModel._class(JMod.PUBLIC, className, EClassType.CLASS);
        webService.annotate(org.springframework.context.annotation.Configuration.class);

        JMethod method = webService.method(JMod.PUBLIC, org.springframework.integration.dsl.IntegrationFlow.class, "fileReadingFlow");
        method.annotate(org.springframework.context.annotation.Bean.class);

        JBlock body = method.body();

        JInvocation fromInvoke = codeModel.ref(org.springframework.integration.dsl.IntegrationFlows.class).staticInvoke("from");

        JInvocation inboundAdapterInvoke = codeModel.ref(org.springframework.integration.file.dsl.Files.class).staticInvoke("inboundAdapter");
        inboundAdapterInvoke.arg(JExpr._new(codeModel.ref(java.io.File.class)).arg("." + File.separator + "files"));
        JInvocation patternFilterInvoke = JExpr.invoke(inboundAdapterInvoke, "patternFilter").arg("*.txt");
        fromInvoke.arg(patternFilterInvoke);


        JVar holder = new JVar (JMods.forVar (0), codeModel.ref (Object.class), "a", null);
        JLambda aLambda = new JLambda ();
        JLambdaParam arr = aLambda.addParam ("a");
        JBlock setBody = aLambda.body ();

        JInvocation fixedDelayInvoke = codeModel.ref(org.springframework.integration.dsl.Pollers.class).staticInvoke("fixedDelay");
        fixedDelayInvoke.arg(1000);

        setBody.add (JExpr.invoke (codeModel, holder, "poller").arg(fixedDelayInvoke));

        fromInvoke.arg(aLambda);

        JInvocation transformInvoke = JExpr.invoke(fromInvoke, "transform");
        JInvocation toStringTransformerInvoke = codeModel.ref(org.springframework.integration.file.dsl.Files.class).staticInvoke("toStringTransformer");
        transformInvoke.arg(toStringTransformerInvoke);

        JInvocation channelInvoke = JExpr.invoke(transformInvoke, "channel").arg("processFileChannel");

        JInvocation getInvoke = JExpr.invoke(channelInvoke, "get");
        body._return(getInvoke);


        method = webService.method(JMod.PUBLIC, org.springframework.messaging.MessageChannel.class, "processFileChannel");
        method.annotate(org.springframework.context.annotation.Bean.class);

        body = method.body();

        JInvocation newChannelInvoque = JExpr._new(codeModel.ref(org.springframework.integration.channel.DirectChannel.class));
        body._return(newChannelInvoque);



        codeModel.build(new File("." + File.separator + "src" + File.separator + "main" + File.separator + "java"));
    }
}
