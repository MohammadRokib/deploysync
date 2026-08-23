package com.deploysync.app;

import com.deploysync.view.support.AppIcon;
import com.deploysync.view.support.HostServicesHolder;
import com.deploysync.view.support.SpringFxmlLoader;
import com.deploysync.view.support.ThemeManager;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
        primaryStage.initStyle(StageStyle.UNDECORATED);

        ThemeManager themeManager = springContext.getBean(ThemeManager.class);
        themeManager.applyCurrentTheme();
        springContext.getBean(HostServicesHolder.class).attach(getHostServices());

        SpringFxmlLoader loader = springContext.getBean(SpringFxmlLoader.class);
        Parent root = loader.load("/com/deploysync/view/shell/MainShell.fxml");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/deploysync/view/shell/app.css").toExternalForm());
        themeManager.attachTo(scene);

        primaryStage.getIcons().addAll(AppIcon.render(16), AppIcon.render(32), AppIcon.render(64), AppIcon.render(128));
        primaryStage.setTitle("DeploySync");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        springContext.close();
    }
}
