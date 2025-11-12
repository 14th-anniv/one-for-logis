package com.oneforlogis.hub.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.application.dto.HubEdge;
import com.oneforlogis.hub.application.dto.DijkstraResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class DijkstraService {

    public DijkstraResult findShortestPath(Map<UUID, List<HubEdge>> graph,
            UUID startHub,
            UUID targetHub) {

        Map<UUID, BigDecimal> distances = new HashMap<>();
        Map<UUID, Integer> times = new HashMap<>();
        Map<UUID, UUID> previous = new HashMap<>();

        PriorityQueue<UUID> pq = new PriorityQueue<>(Comparator.comparing(distances::get));

        for (UUID hubId : graph.keySet()) {
            distances.put(hubId, BigDecimal.valueOf(Double.MAX_VALUE));
            times.put(hubId, Integer.MAX_VALUE);
        }

        distances.put(startHub, BigDecimal.ZERO);
        times.put(startHub, 0);
        pq.add(startHub);

        while (!pq.isEmpty()) {
            UUID current = pq.poll();
            if (!graph.containsKey(current)) continue;

            for (HubEdge edge : graph.get(current)) {
                HubEdge currentEdge = new HubEdge(current, edge.toHubId(), edge.routeDistance(), edge.routeTime());

                UUID neighbor = currentEdge.toHubId();
                BigDecimal newDist = distances.get(current).add(currentEdge.routeDistance());
                int newTime = times.get(current) + currentEdge.routeTime();

                if (newDist.compareTo(distances.get(neighbor)) < 0) {
                    distances.put(neighbor, newDist);
                    times.put(neighbor, newTime);
                    previous.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        if (!previous.containsKey(targetHub)) throw new CustomException(ErrorCode.HUB_ROUTE_PATH_NOT_FOUND);

        List<UUID> path = new ArrayList<>();
        List<HubEdge> edges = new ArrayList<>();
        UUID current = targetHub;

        while (previous.containsKey(current)) {
            UUID prev = previous.get(current);
            path.add(current);
            UUID curr = current;
            graph.get(prev).stream()
                    .filter(e -> e.toHubId().equals(curr))
                    .findFirst()
                    .ifPresent(edges::add);
            current = prev;
        }

        Collections.reverse(path);
        Collections.reverse(edges);

        return new DijkstraResult(distances.get(targetHub), times.get(targetHub), path, edges);
    }
}