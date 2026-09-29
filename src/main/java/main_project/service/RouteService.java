package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.RouteRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class RouteService {

    private final RouteRepository routeRepository;




}
