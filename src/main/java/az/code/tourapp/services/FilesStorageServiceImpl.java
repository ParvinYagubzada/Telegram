package az.code.tourapp.services;

import az.code.tourapp.models.entities.Offer;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    private final Path root = Paths.get("uploads");

    @Override
    public void init() {
        try {
            FileUtils.deleteDirectory(root.toFile());
            Files.createDirectory(root);
        } catch (IOException ignored) {
            System.out.println("Cant delete or create LINE-29");
        }
    }

    @Override
    public void save(InputStream file, String fileName) {
        try {
            Files.copy(file, this.root.resolve(fileName));
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void delete(String filename) {
        Path file = root.resolve(filename);
        try {
            Files.delete(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAll(Iterable<Offer> offers) {
        for (Offer offer : offers) {
            Path file = root.resolve(offer.getPhotoUrl());
            try {
                Files.delete(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
