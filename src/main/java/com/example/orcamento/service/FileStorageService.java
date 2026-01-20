package com.example.orcamento.service;

import com.example.orcamento.config.FileStorageProperties;
import com.example.orcamento.exception.FileStorageException;
import com.example.orcamento.exception.MyFileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Não foi possível criar o diretório para armazenar os arquivos.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Normaliza o nome do arquivo
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            // Verifica se o nome do arquivo contém caracteres inválidos
            if(originalFileName.contains("..")) {
                throw new FileStorageException("Desculpe! O nome do arquivo contém uma sequência de caminho inválida " + originalFileName);
            }
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Copia o arquivo para o local de destino (substituindo o arquivo existente com o mesmo nome)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível armazenar o arquivo " + originalFileName + ". Por favor, tente novamente!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("Arquivo não encontrado " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("Arquivo não encontrado " + fileName, ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if(Files.exists(filePath)){
                Files.delete(filePath);
            }
        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível deletar o arquivo " + fileName + ". Por favor, tente novamente!", ex);
        }
    }
}
