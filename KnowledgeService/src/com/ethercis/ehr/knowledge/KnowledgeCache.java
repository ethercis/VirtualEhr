package com.ethercis.ehr.knowledge;

import com.ethercis.servicemanager.service.ServiceInfo;
import openEHR.v1.template.TEMPLATE;
import openEHR.v1.template.TemplateDocument;
import org.apache.log4j.Logger;
import org.openehr.am.archetype.Archetype;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import se.acode.openehr.parser.ADLParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * KnowledgeCache implements knowledge look up and caching for
 * archetype in ordered by <p>
 * <ul> 1. Using system environment %ETHERCIS_HOME%/archetypes/
 * <ul> 2. Application path %USER_HOME%/.ethercis/archetypes 
 * <ul> 3. User can also include archetype by call addXYZPath method
 * 
 * @author ADOC01
 * 
 */
public class KnowledgeCache implements I_KnowledgeCache {

	private Map<String, File> archetypeFileMap = new HashMap<String, File>();
	private Map<String, Archetype> atArchetypeCache = new HashMap<String, Archetype>();
	private Map<String, File> templatesFileMap = new HashMap<String, File>();
	private Map<String, TEMPLATE> atTemplatesCache = new HashMap<String, TEMPLATE>();
	private Map<String, File> optFileMap = new HashMap<String, File>();
	private Map<String, OPERATIONALTEMPLATE> atOptCache = new HashMap<String, OPERATIONALTEMPLATE>();
	
	public enum KnowledgeType {
		ARCHETYPE ("ARCHETYPES", "adl"), 
		TEMPLATE("TEMPLATE", "oet"), 
		OPT("OPT", "opt");
		
		private String qualifier;
		private String extension;
		
		KnowledgeType(String qualifier, String extension){
			this.qualifier = qualifier;
			this.extension = extension;
		}
		
		public String getQualifier() { return qualifier; }
		public String getExtension() { return extension; }
	}
	
	public static String DEFAULT_ENCODING = "UTF-8";

	private static Logger log = Logger.getLogger(KnowledgeCache.class);

	public KnowledgeCache() {
		
		for (KnowledgeType k: KnowledgeType.values()) {
			String env = "ETHERCIS_"+k.getQualifier();
			if (System.getenv(env) != null) {
				String systemEnvDir = System.getenv(env)
						+ File.separator + "repository"+ File.separator + "archetypes";
				try {
					if (new File(systemEnvDir).exists()) {
						log.debug("mapping knowledge dir:"+systemEnvDir);
						addKnowledgeSourcePath(systemEnvDir, k);
					}
				} catch (IOException e) {
					log.warn("Some default path not found");
				} catch (Exception e) {
					log.warn("addPath",e);

				}
			}
		}

		String localDirPrefix = System.getProperty("user.home")
				+ File.separator + ".ethercis" + File.separator;
		for (KnowledgeType k: KnowledgeType.values()) {
			String dir = localDirPrefix+k.getQualifier().toLowerCase();
			try {
				if (new File(dir).exists()) {
					log.debug("mapping knowledge dir:"+dir);
					addKnowledgeSourcePath(dir, k);
				}
			} catch (Exception e) {
				log.warn(e.getMessage());
				// e.printStackTrace();
			}
		}
	}

	public KnowledgeCache(String path, KnowledgeType what) {
		this();
		try {
			switch (what) {
			case ARCHETYPE:
				addKnowledgeSourcePath(path, archetypeFileMap, what.getExtension());
				break;
			case TEMPLATE:
				addKnowledgeSourcePath(path, templatesFileMap, what.getExtension());
				break;
			case OPT:
				addKnowledgeSourcePath(path, optFileMap, what.getExtension());
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * initialize with properties passed from a ServiceInfo context
	 * @param props
	 * @see ServiceInfo
	 */
	public KnowledgeCache(Properties props) {
		this();
		for (Entry<Object, Object> entry: props.entrySet()) {
			if ( entry != null && ((String)entry.getKey()).compareTo("knowledge.path.archetypes")==0) {
				try {
					log.debug("mapping knowledge dir:"+((String)entry.getValue()));
					addKnowledgeSourcePath(((String) entry.getValue()), KnowledgeType.ARCHETYPE);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if ( entry != null && ((String)entry.getKey()).compareTo("knowledge.path.templates")==0) {
				try {
					log.debug("mapping knowledge dir:"+(entry.getValue()));
					addKnowledgeSourcePath(((String) entry.getValue()), KnowledgeType.TEMPLATE);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public File getFile(String key) {
		File ret= archetypeFileMap.get(key);
		return ret;
	}
	
	public File getFile(String key, KnowledgeType what) {

		switch(what) {
			case ARCHETYPE: return archetypeFileMap.get(key);
			case TEMPLATE: return templatesFileMap.get(key);
			case OPT: return optFileMap.get(key);
		}
		return null;
	}

	public Map<String, File> getFiles(Pattern includes, Pattern excludes) {
		Map<String, File> mf = new LinkedHashMap<String, File>();
		if (includes != null) {
			for (String s : archetypeFileMap.keySet()) {
				if (includes.matcher(s).find()) {
					mf.put(s, archetypeFileMap.get(s));
				}
			}
		} else {
			mf.putAll(archetypeFileMap);
		}

		if (excludes != null) {
			List<String> removeList = new ArrayList<String>();
			for (String s : mf.keySet()) {
				if (excludes.matcher(s).find()) {
					removeList.add(s);
				}
			}
			for (String s : removeList) {
				mf.remove(s);
			}
		}
		return mf;
	}

	public Archetype getArchetype(String key) throws Exception {
		log.debug("getArchetype(" + key + ")");
		Archetype at = atArchetypeCache.get(key);
		if (at == null) {
			InputStream in = null;
			try {
				in = getStream(key, KnowledgeType.ARCHETYPE);
				at = new ADLParser(in, DEFAULT_ENCODING).parse();
				atArchetypeCache.put(key, at);
			} catch (Exception e) {
				log.error("getArchetype (" + key + ") error", e);
			} finally {
				if (in != null)
					in.close();
			}
		}
		return at;
	}
	
	public TEMPLATE retrieveTemplate(String key) throws Exception {
		log.debug("retrieveTemplate(" + key + ")");
		TEMPLATE template = atTemplatesCache.get(key);
		if (template == null) {
			InputStream in = null;
			try {
				in = getStream(key, KnowledgeType.TEMPLATE);
				TemplateDocument tdoc = TemplateDocument.Factory.parse(in);
				template = tdoc.getTemplate();
				atTemplatesCache.put(key, template);
			} catch (Exception e) {
				log.error("retrieveTemplate (" + key + ") error", e);
			} finally {
				if (in != null)
					in.close();
			}
		}
		
		return template;
	}
	
	public OPERATIONALTEMPLATE retrieveOperationalTemplate(String key) throws Exception {
		log.debug("retrieveOperationalTemplate(" + key + ")");
		OPERATIONALTEMPLATE template = atOptCache.get(key);
		if (template == null) {
			InputStream in = null;
			try {
				in = getStream(key, KnowledgeType.OPT);
				template = OPERATIONALTEMPLATE.Factory.parse(in);
				atOptCache.put(key, template);
			} catch (Exception e) {
				log.error("retrieveOperationalTemplate (" + key + ") error", e);
			} finally {
				if (in != null)
					in.close();
			}
		}
		return template;
	}

	public List<Archetype> retrieveArchetypes(Pattern includes, Pattern excludes)
			throws Exception {
		log.debug("getArchetype(" + includes + ", " + excludes + ")");
		List<Archetype> atList = new ArrayList<Archetype>();
		Map<String, File> files = getFiles(includes, excludes);
		for (Entry<String, File> entry : files.entrySet()) {
			atList.add(getArchetype(entry.getKey()));
		}
		return atList;
	}

	@SuppressWarnings("resource")
	private InputStream getStream(String key, KnowledgeType what) throws IOException {
		File file = getFile(key, what);
		return file != null ? new FileInputStream(file) : null;
	}
	
	public boolean addKnowledgeSourcePath(String path, KnowledgeType what) throws Exception {
		switch(what) {
			case ARCHETYPE: return addKnowledgeSourcePath(path, archetypeFileMap, what.getExtension());
			case TEMPLATE: return addKnowledgeSourcePath(path, templatesFileMap, what.getExtension());
			case OPT: return addKnowledgeSourcePath(path, optFileMap, what.getExtension());
		}
		
		return false;
	}

	public boolean addKnowledgeSourcePath(String path, Map<String, File> resource, String extension) throws Exception {
		if (path == null) return false;

		path = path.trim();
		if (path.isEmpty())
			throw new IllegalArgumentException("Source path is empty!");

		File root = new File(path);

		root = new File(root.getAbsolutePath());

		if (!root.isDirectory())
			throw new IllegalArgumentException("Supplied source path:"+ path + "("
					+ root.getAbsolutePath() + ") is not a directory!");

		List<File> tr = new ArrayList<File>();
		tr.add(root);
		while (tr.size() > 0) {
			File r = tr.remove(tr.size() - 1);
			for (File f : r.listFiles()) {
				if (f.isHidden())
					continue;

				if (f.isFile()) {
					String key = f.getName().replaceAll("([^\\\\\\/]+)\\."+extension,
							"$1");
					//System.out.println("key=" + key + ":" +f);
					resource.put(key, f);
				} else if (f.isDirectory()) {
					tr.add(f);
				}
			}
		}
		return true;
	}

	@Override
	public Map<String, Archetype> getArchetypeMap() {
		return atArchetypeCache;
	}

}
