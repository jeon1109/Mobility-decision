package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.io.StringReader;
import java.time.Instant;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
@Component
public class TopisXmlParser {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(TopisXmlParser.class);
    public TopisResponseDto parse(String xml,String service,Instant collectedAt){
        try{
            if(xml==null||xml.isBlank()) throw new TopisException(false);
            var factory=DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            var builder=factory.newDocumentBuilder();builder.setErrorHandler(new DefaultHandler());
            var document=builder.parse(new InputSource(new StringReader(xml)));
            String code=text(document.getDocumentElement(),"CODE");
            if("INFO-200".equals(code)) return new TopisResponseDto(0,List.of(),collectedAt,true);
            if(!"INFO-000".equals(code)) {
                log.warn("[TOPIS] result=API_ERROR apiCode={}",code.matches("[A-Z]{1,12}-[0-9]{3}")?code:"UNKNOWN");
                throw new TopisException(Set.of("ERROR-500","ERROR-600","ERROR-601").contains(code));
            }
            if(!service.equals(document.getDocumentElement().getTagName())) throw new TopisException(false);
            int total=Integer.parseInt(text(document.getDocumentElement(),"list_total_count"));
            if(total<0) throw new TopisException(false);
            List<Map<String,String>> rows=new ArrayList<>();
            NodeList nodes=document.getElementsByTagName("row");
            for(int i=0;i<nodes.getLength();i++){
                Map<String,String> row=new LinkedHashMap<>();
                NodeList fields=nodes.item(i).getChildNodes();
                for(int j=0;j<fields.getLength();j++) if(fields.item(j) instanceof Element field)
                    row.put(field.getTagName().toUpperCase(Locale.ROOT),field.getTextContent().trim());
                rows.add(Map.copyOf(row));
            }
            if(rows.isEmpty()&&total>0) throw new TopisException(false);
            return new TopisResponseDto(total,List.copyOf(rows),collectedAt,rows.size()>=total);
        }catch(TopisException e){throw e;}catch(Exception e){throw new TopisException(false);}
    }
    private String text(Element element,String name){
        NodeList nodes=element.getElementsByTagName(name);
        return nodes.getLength()==0?"":nodes.item(0).getTextContent().trim();
    }
}
