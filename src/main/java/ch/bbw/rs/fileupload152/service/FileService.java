package ch.bbw.rs.fileupload152.service;

import ch.bbw.rs.fileupload152.model.File;
import ch.bbw.rs.fileupload152.repository.FileRepository;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

@Service
public class FileService {
    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public void store(MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename()).split("\\.")[0];

        try {
            fileRepository.save(new File(fileName + ".png", "image/png", createThumbnail(file)));
            fileRepository.save(new File(fileName + ".jpg", "image/jpeg", addWatermark(file)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ImageReader getReader(MultipartFile file) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByFormatName(file.getContentType().replace("image/", "")).next();
        reader.setInput(ImageIO.createImageInputStream(file.getInputStream()));

        return reader;
    }

    private BufferedImage getBufferedImage(ImageReader reader, String contentType, String outputType) throws IOException {
        int imgWidth = reader.getWidth(0);
        int imgHeight = reader.getHeight(0);
        int type = BufferedImage.TYPE_INT_RGB;

        if (contentType.equals("image/png") && outputType.equals("png")) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        if (outputType.equals("png")) {
            imgWidth = reader.getWidth(0) / 8;
            imgHeight = reader.getHeight(0) / 8;
        }

        return new BufferedImage(imgWidth, imgHeight, type);
    }

    private byte[] convertToByteArray(BufferedImage bufferedImage, ImageReader reader, String format, ImageWriteParam imageWriteParam) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        IIOMetadata metadata = reader.getImageMetadata(0);
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(format);
        ImageWriter writer = iter.next();

        if (imageWriteParam == null) {
            imageWriteParam = writer.getDefaultWriteParam();
        }

        if (metadata.getMetadataFormatNames()[0].contains("png")) {
            metadata = null;
        }

        writer.setOutput(ios);
        writer.write(null, new IIOImage(bufferedImage, null, metadata), imageWriteParam);
        writer.dispose();

        return os.toByteArray();
    }

    private byte[] createThumbnail(MultipartFile file) throws IOException, ImageProcessingException, MetadataException {
        ImageReader reader = getReader(file);
        BufferedImage bufferedImage = getBufferedImage(reader, file.getContentType(), "png");
        Graphics2D graphics = bufferedImage.createGraphics();

        int orientation = 0;
        int degrees = 0;

        try {
            orientation = getImageOrientation(file.getBytes());
        } catch (Exception e) {
        }

        switch (orientation) {
            case 6:
                degrees = 90;
                break;
        }

        graphics.rotate(Math.toRadians(degrees));
        graphics.drawImage(reader.read(0), 0, 0, reader.getWidth(0) / 8, reader.getHeight(0) / 8, null);
        graphics.dispose();

        return convertToByteArray(bufferedImage, reader, "png", null);
    }

    private byte[] addWatermark(MultipartFile file) throws IOException, ImageProcessingException {
        ImageReader reader = getReader(file);
        BufferedImage bufferedImage = getBufferedImage(reader, file.getContentType(), "jpg");
        Graphics2D graphics = bufferedImage.createGraphics();

        String mark = "Forest Adventures";
        Font font = new Font("Arial", Font.BOLD, 100);

        int orientation = 0;
        int degrees = 0;

        try {
            orientation = getImageOrientation(file.getBytes());
        } catch (Exception e) {
        }

        switch (orientation) {
            case 6:
                degrees = 90;
                break;
        }

        graphics.rotate(Math.toRadians(degrees));
        graphics.drawImage(reader.read(0), 0, 0, null);
        graphics.setColor(Color.white);
        graphics.setFont(font);
        graphics.drawString(mark, 100, 155);
        graphics.dispose();

        return convertToByteArray(bufferedImage, reader, "jpg", null);
    }

    private int getImageOrientation(byte[] imageContent) throws ImageProcessingException, IOException, MetadataException, NullPointerException {
        final Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageContent));
        final ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        return exifDirectory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
    }

    public File getFile(int id) {
        return fileRepository.findById(id).get();
    }

    public Stream<File> getAllFiles() {
        return fileRepository.findAll().stream();
    }
}