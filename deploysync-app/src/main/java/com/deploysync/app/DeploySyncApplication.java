package com.deploysync.app;

import com.deploysync.view.support.SpringFxmlLoader;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "com.deploysync")
public class DeploySyncApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        springContext = new SpringApplicationBuilder(DeploySyncApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SpringFxmlLoader loader = springContext.getBean(SpringFxmlLoader.class);
        Parent root = loader.load("/com/deploysync/view/shell/MainShell.fxml");

        primaryStage.setTitle("DeploySync");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        springContext.close();
    }
}
