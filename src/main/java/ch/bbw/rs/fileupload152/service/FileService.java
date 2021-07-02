package ch.bbw.rs.fileupload152.service;

import ch.bbw.rs.fileupload152.model.File;
import ch.bbw.rs.fileupload152.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
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

        //fileRepository.save(new File(fileName + ".png", "image/png", createThumbnail(file)));
        fileRepository.save(new File(fileName + ".jpg", "image/jpeg", addWatermark(file)));
    }

    private ImageReader getReader(MultipartFile file) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByFormatName(file.getContentType().replace("image/", "")).next();
        reader.setInput(ImageIO.createImageInputStream(file.getInputStream()));

        return reader;
    }

    private BufferedImage getBufferedImage(ImageReader reader) throws IOException {
        int imgWidth = reader.getWidth(0);
        int imgHeight = reader.getHeight(0);

        return new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
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

        writer.setOutput(ios);
        writer.write(null, new IIOImage(bufferedImage, null, metadata), imageWriteParam);
        writer.dispose();

        return os.toByteArray();
    }

    private byte[] createThumbnail(MultipartFile file) throws IOException {
        ImageReader reader = getReader(file);
        BufferedImage bufferedImage = getBufferedImage(reader);
        Graphics2D graphics = bufferedImage.createGraphics();

        graphics.drawImage(reader.read(0), 0, 0, reader.getWidth(0), reader.getHeight(0), null);
        graphics.dispose();

        return convertToByteArray(bufferedImage, reader, "png", null);
    }

    private byte[] addWatermark(MultipartFile file) throws IOException {
        ImageReader reader = getReader(file);
        BufferedImage bufferedImage = getBufferedImage(reader);
        Graphics2D graphics = bufferedImage.createGraphics();

        String mark = "Forest Adventures";
        Font font = new Font("Arial", Font.BOLD, 100);

        graphics.drawImage(reader.read(0), 0, 0, reader.getWidth(0), reader.getHeight(0), null);
        graphics.setColor(Color.white);
        graphics.setFont(font);
        graphics.drawString(mark, 100, 150);
        graphics.dispose();

        return convertToByteArray(bufferedImage, reader, "jpg", null);
    }

    public File getFile(int id) {
        return fileRepository.findById(id).get();
    }

    public Stream<File> getAllFiles() {
        return fileRepository.findAll().stream();
    }
}