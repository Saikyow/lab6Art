package lab6.server.io;

import lab6.common.model.City;
import lab6.common.model.Coordinates;
import lab6.common.model.Human;
import lab6.common.model.StandardOfLiving;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Загрузка и сохранение коллекции городов в XML-файле.
 * <p>Чтение — через {@link FileReader} (DOM-парсер на основе {@link InputSource}),
 * запись — через {@link FileOutputStream} (сериализация вручную в UTF-8).
 */
public final class XmlCityRepository {

    private final Path filePath;

    /**
     * @param filePath абсолютный путь к XML-файлу с коллекцией
     */
    public XmlCityRepository(Path filePath) {
        this.filePath = filePath;
    }

    /** @return путь к файлу с коллекцией */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Загружает коллекцию из файла.
     * Если файл не существует — возвращает пустой список (это валидный старт с нуля).
     *
     * @return список загруженных городов
     * @throws XmlRepositoryException при ошибке чтения или невалидной структуре XML
     */
    public List<City> load() throws XmlRepositoryException {
        try (FileReader reader = new FileReader(filePath.toFile(), StandardCharsets.UTF_8)) {
            DocumentBuilder builder = newDocumentBuilder();
            Document document = builder.parse(new InputSource(reader));
            return parseCities(document);
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            throw new XmlRepositoryException("Не удалось прочитать файл: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new XmlRepositoryException("Файл повреждён или невалиден: " + e.getMessage(), e);
        }
    }

    /**
     * Сохраняет коллекцию в файл (перезаписывает).
     *
     * @param cities коллекция городов; не {@code null}
     * @throws XmlRepositoryException при ошибке записи
     */
    public void save(Collection<City> cities) throws XmlRepositoryException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<cities>\n");
        for (City city : cities) {
            appendCity(xml, city);
        }
        xml.append("</cities>\n");

        try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
            out.write(xml.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new XmlRepositoryException("Не удалось записать файл: " + e.getMessage(), e);
        }
    }

    private DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        return factory.newDocumentBuilder();
    }

    private List<City> parseCities(Document doc) {
        List<City> result = new ArrayList<>();
        Element root = doc.getDocumentElement();
        if (root == null || !"cities".equals(root.getNodeName())) {
            throw new IllegalArgumentException("корневой элемент должен называться <cities>");
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            if (!"city".equals(node.getNodeName())) continue;
            result.add(parseCity((Element) node));
        }
        return result;
    }

    private City parseCity(Element el) {
        int id = parseInt(requireText(el, "id"), "id");
        String name = requireText(el, "name");
        Coordinates coordinates = parseCoordinates(requireChild(el, "coordinates"));
        ZonedDateTime creationDate = parseDate(requireText(el, "creationDate"));
        Double area = parseDouble(requireText(el, "area"), "area");
        Long population = parseLong(requireText(el, "population"), "population");
        int metersAboveSeaLevel = parseInt(requireText(el, "metersAboveSeaLevel"), "metersAboveSeaLevel");
        double timezone = parseDouble(requireText(el, "timezone"), "timezone");
        int carCode = parseInt(requireText(el, "carCode"), "carCode");
        StandardOfLiving sol = parseStandardOfLiving(optionalText(el, "standardOfLiving"));
        Human governor = new Human(requireText(requireChild(el, "governor"), "name"));
        return new City(id, name, coordinates, creationDate, area, population,
                metersAboveSeaLevel, timezone, carCode, sol, governor);
    }

    private Coordinates parseCoordinates(Element el) {
        long x = parseLong(requireText(el, "x"), "coordinates.x");
        Float y = Float.parseFloat(requireText(el, "y"));
        return new Coordinates(x, y);
    }

    private ZonedDateTime parseDate(String raw) {
        try {
            return ZonedDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Невалидная дата: " + raw, e);
        }
    }

    private StandardOfLiving parseStandardOfLiving(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        return StandardOfLiving.valueOf(raw);
    }

    private static Element requireChild(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            throw new IllegalArgumentException("отсутствует обязательный тег <" + tag + ">");
        }
        return (Element) list.item(0);
    }

    private static String requireText(Element parent, String tag) {
        String text = optionalText(parent, tag);
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("пустой или отсутствующий тег <" + tag + ">");
        }
        return text;
    }

    private static String optionalText(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;
        String text = list.item(0).getTextContent();
        return text == null ? null : text.trim();
    }

    private static int parseInt(String raw, String field) {
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("невалидное число для " + field + ": " + raw, e); }
    }

    private static long parseLong(String raw, String field) {
        try { return Long.parseLong(raw); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("невалидное число для " + field + ": " + raw, e); }
    }

    private static double parseDouble(String raw, String field) {
        try { return Double.parseDouble(raw); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("невалидное число для " + field + ": " + raw, e); }
    }

    private void appendCity(StringBuilder xml, City c) {
        xml.append("    <city>\n");
        appendElement(xml, "id", String.valueOf(c.getId()));
        appendElement(xml, "name", c.getName());
        xml.append("        <coordinates>\n");
        appendElement(xml, "x", String.valueOf(c.getCoordinates().getX()), 12);
        appendElement(xml, "y", String.valueOf(c.getCoordinates().getY()), 12);
        xml.append("        </coordinates>\n");
        appendElement(xml, "creationDate", c.getCreationDate().toString());
        appendElement(xml, "area", String.valueOf(c.getArea()));
        appendElement(xml, "population", String.valueOf(c.getPopulation()));
        appendElement(xml, "metersAboveSeaLevel", String.valueOf(c.getMetersAboveSeaLevel()));
        appendElement(xml, "timezone", String.valueOf(c.getTimezone()));
        appendElement(xml, "carCode", String.valueOf(c.getCarCode()));
        if (c.getStandardOfLiving() != null) {
            appendElement(xml, "standardOfLiving", c.getStandardOfLiving().name());
        }
        xml.append("        <governor>\n");
        appendElement(xml, "name", c.getGovernor().getName(), 12);
        xml.append("        </governor>\n");
        xml.append("    </city>\n");
    }

    private static void appendElement(StringBuilder xml, String tag, String value) {
        appendElement(xml, tag, value, 8);
    }

    private static void appendElement(StringBuilder xml, String tag, String value, int indent) {
        xml.append(" ".repeat(indent))
                .append('<').append(tag).append('>')
                .append(escape(value))
                .append("</").append(tag).append(">\n");
    }

    private static String escape(String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
