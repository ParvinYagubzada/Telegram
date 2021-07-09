package az.code.tourapp.services;

import az.code.tourapp.models.entities.Offer;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface FilesStorageService {
    void init();

    void save(InputStream file, String fileName);

    Resource load(String filename);

    void delete(String filename);

    void deleteAll(Iterable<Offer> offers);

    Stream<Path> loadAll();
}