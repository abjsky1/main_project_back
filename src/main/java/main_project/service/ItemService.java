package main_project.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import main_project.model.repository.ItemRepository;

@Service 
@RequiredArgsConstructor 
@Transactional 
public class ItemService {

    private final ItemRepository itemRepository;




    
}
