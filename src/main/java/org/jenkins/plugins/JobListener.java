package org.jenkins.plugins;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import com.alibaba.fastjson.JSON;
import okhttp3.*;

import javax.annotation.Nonnull;

@Extension
public class JobListener extends RunListener<AbstractBuild> {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client;

    public JobListener() {
        super(AbstractBuild.class);
        client = new OkHttpClient();
    }

    @Override
    public void onStarted(AbstractBuild build, TaskListener listener) {
        WebHookPublisher publisher = GetWebHookPublisher(build);
        if (publisher == null || !publisher.onStart) {
            return;
        }
        String webHookUrl = publisher.webHookUrl;
        String buildUrl = build.getAbsoluteUrl();
        String projectName = build.getProject().getDisplayName();
        String buildName = build.getDisplayName();
        NotificationEvent event = new NotificationEvent(projectName, buildName, buildUrl, "start");
        event.text = projectName + ":" + buildName + " has just started at " +buildUrl;
        httpPost(webHookUrl, event);
    }

    @Override
    public void onCompleted(AbstractBuild build, @Nonnull TaskListener listener) {
        WebHookPublisher publisher = GetWebHookPublisher(build);
        if (publisher == null) {
            return;
        }
        Result result = build.getResult();
        if (result == null) {
            return;
        }
        String webHookUrl = publisher.webHookUrl;
        String buildUrl = build.getAbsoluteUrl();
        String projectName = build.getProject().getDisplayName();
        String buildName = build.getDisplayName();
        NotificationEvent event = new NotificationEvent(projectName, buildName, buildUrl, "");
        if (publisher.onSuccess && result.equals(Result.SUCCESS)) {
            event.event = "success";
            event.text = projectName + ":" + buildName + " was sucessful at " +buildUrl;
            httpPost(webHookUrl, event);
        }
        if (publisher.onFailure && result.equals(Result.FAILURE)) {
            event.event = "failure";
            event.text = projectName + ":" + buildName + " failed at " +buildUrl;
            httpPost(webHookUrl, event);
        }
    }

    private WebHookPublisher GetWebHookPublisher(AbstractBuild build) {
        for (Object publisher : build.getProject().getPublishersList().toMap().values()) {
            if (publisher instanceof WebHookPublisher) {
                return (WebHookPublisher) publisher;
            }
        }
        return null;
    }

    private void httpPost(String url, Object object) {
        String jsonString = JSON.toJSONString(object);
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonString);
        Request request = new Request.Builder().url(url).post(body).build();
        try {
            Response response = client.newCall(request).execute();
        } catch (Exception e) {
        }
    }
}
