package com.ethercis.ehr.knowledge;

import com.ethercis.ehr.knowledge.KnowledgeCache.KnowledgeType;
import openEHR.v1.template.TEMPLATE;
import org.openehr.am.archetype.Archetype;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;



public interface I_KnowledgeCache {
	
	public File getFile(String key);

    /**
     * return a map of identifier and File for a defined search pattern.
     * @param includes
     * @param excludes
     * @return
     */
	public Map<String, File> getFiles(Pattern includes, Pattern excludes);

	public Archetype getArchetype(String key) throws Exception;
	
	public TEMPLATE retrieveTemplate(String key) throws Exception;
	
	public OPERATIONALTEMPLATE retrieveOperationalTemplate(String key) throws Exception;

	public List<Archetype> retrieveArchetypes(Pattern includes, Pattern excludes)
			throws Exception;
	
	public boolean addKnowledgeSourcePath(String path, KnowledgeType type) throws Exception;
	
	public Map<String, Archetype> getArchetypeMap();

}
