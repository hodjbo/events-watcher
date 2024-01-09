package com.hodbenor.project.eventwatcher.service;

import com.hodbenor.project.eventwatcher.service.beans.EventKey;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class KubernetesEventWatcher {
    private final Map<EventKey, Integer> events = new HashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${url}")
    private String url;

    public KubernetesEventWatcher() {
        new Thread(this::listenEvent).start();
    }


    public void listenEvent() {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            SharedInformerFactory informerFactory = client.informers();
            SharedIndexInformer<Pod> podInformer = informerFactory.sharedIndexInformerFor(Pod.class,
                    Duration.ofMinutes(1).toMillis());

            podInformer.addEventHandler(new ResourceEventHandler<Pod>() {

                @Override
                public void onAdd(Pod pod) {
                    handleEvent("POD", "ADDED");
                }

                @Override
                public void onUpdate(Pod pod, Pod t1) {
                    handleEvent("POD", "MODIFIED");
                }

                @Override
                public void onDelete(Pod pod, boolean b) {
                    handleEvent("POD", "DELETED");
                }
            }).start();

            SharedIndexInformer<Deployment> deploymentInformer = informerFactory.sharedIndexInformerFor(Deployment.class,
                    Duration.ofMinutes(1).toMillis());

            deploymentInformer.addEventHandler(new ResourceEventHandler<Deployment>() {
                @Override
                public void onAdd(Deployment deployment) {
                    handleEvent("DEPLOYMENT", "ADDED");
                }

                @Override
                public void onUpdate(Deployment deployment, Deployment t1) {
                    handleEvent("DEPLOYMENT", "MODIFIED");
                }

                @Override
                public void onDelete(Deployment deployment, boolean b) {
                    handleEvent("DEPLOYMENT", "DELETED");
                }
            }).start();

            SharedIndexInformer<Service> serviceInformer = informerFactory.sharedIndexInformerFor(Service.class,
                    Duration.ofMinutes(1).toMillis());

            serviceInformer.addEventHandler(new ResourceEventHandler<Service>() {
                @Override
                public void onAdd(Service service) {
                    handleEvent("SERVICE", "ADDED");
                }

                @Override
                public void onUpdate(Service service, Service t1) {
                    handleEvent("SERVICE", "MODIFIED");
                }

                @Override
                public void onDelete(Service deployment, boolean b) {
                    handleEvent("SERVICE", "DELETED");
                }
            }).start();

            while (!Thread.currentThread().isInterrupted()) {
                TimeUnit.MINUTES.sleep(1);
            }

            informerFactory.startAllRegisteredInformers();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void handleEvent(String resourceType, String eventType) {
        EventKey eventKey = new EventKey(resourceType, eventType);
        synchronized (events) {
            int eventCounter = events.getOrDefault(eventKey, 0);
            events.put(eventKey, eventCounter + 1);
        }

    }

    @Scheduled(fixedDelay = 60000)
    private void sendEvents() {
        synchronized (events) {
            if (!events.isEmpty()) {
                JSONObject jsonObject = new JSONObject();
                events.forEach((key, value) -> {
                    String event = key.eventType() + " " + key.resourceType();
                    jsonObject.put(event, value);
                });
                restTemplate.postForEntity(url, jsonObject, Void.class);
                events.clear();
            }
        }
    }
}
