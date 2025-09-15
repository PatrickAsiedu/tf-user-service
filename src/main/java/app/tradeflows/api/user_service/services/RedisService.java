package app.tradeflows.api.user_service.services;

import app.tradeflows.api.user_service.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RedisService<T> {

    @Autowired
    private RedisTemplate<String, T> redisTemplate;

    public T getItem(String key) throws NotFoundException {
        T value = redisTemplate.opsForValue().get(key);
        if(Objects.isNull(value)){
            throw new NotFoundException("Record found for this key: "+key);
        }
        return value;
    }

    public void addItem(String key, T object) {
        redisTemplate.opsForValue().set(key, object);
    }

    public Boolean deleteItem(String key){
        return redisTemplate.delete(key);
    }

}
