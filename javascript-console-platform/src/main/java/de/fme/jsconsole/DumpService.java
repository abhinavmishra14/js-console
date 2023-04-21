package de.fme.jsconsole;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.jscript.ScriptLogger;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.lock.mem.LockState;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.webdav.WebDavService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.ScriptValueConverter;

/**
 * Implements the 'dumpService' JavaScript extension object that is available in
 * the JavaScript Console and is used for quickly dumping the state of nodes in
 * lieu of inspecting it via the Node Browser.
 *
 * @author Florian Maul (fme AG)
 *
 */
public class DumpService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptLogger.class);

	/** The node service. */
	private NodeService nodeService;

	/** The permission service. */
	private PermissionService permissionService;

	/** The namespace service. */
	private NamespaceService namespaceService;

	/** The version service. */
	private VersionService versionService;

	/** The content service. */
	private ContentService contentService;

	/** The dictionary service. */
	private DictionaryService dictionaryService;

	/** The rule service. */
	private RuleService ruleService;

	/** The workflow service. */
	private WorkflowService workflowService;

	/** The tag service. */
	private TaggingService tagService;

	/** The web dav service. */
	private WebDavService webDavService;

	/** The audit service. */
	private AuditService auditService;

	/** The sys admin params. */
	private SysAdminParams sysAdminParams;

	/** The dump counter. */
	private AtomicInteger dumpCounter = new AtomicInteger();

	/** The dump limit. */
	private int dumpLimit;

	/** The lock service. */
	private LockService lockService;

	/**
	 * Adds the dump.
	 *
	 * @param obj the obj
	 * @return the list
	 */
	public List<JsConsoleDump> addDump(Object obj) {
		List<JsConsoleDump> dumpOutput = new LinkedList<>();

		if (obj != null) {
			Object value = ScriptValueConverter.unwrapValue(obj);

			if (value instanceof Collection<?>) {
				Collection<?> col = (Collection<?>) value;
				Iterator<?> colIter = col.iterator();
				int currentValue = dumpCounter.get();
				while (colIter.hasNext()) {
					if (dumpLimit == -1 || currentValue <= dumpLimit) {
						dumpOutput.add(dumpObject(colIter.next()));
						currentValue = dumpCounter.incrementAndGet();
					} else {
						LOGGER.warn("Reached dump limit");
					}
				}
			} else {
				int currentValue = dumpCounter.getAndIncrement();
				if (dumpLimit == -1 || currentValue <= dumpLimit) {
					dumpOutput.add(dumpObject(value));
				} else {
					LOGGER.warn("Reached dump limit");
				}
			}
		}
		return dumpOutput;
	}

	/**
	 * Dump object.
	 *
	 * @param value the value
	 * @return the dump
	 */
	private JsConsoleDump dumpObject(Object value) {
		final NodeRef nodeRef = extractNodeRef(value);

		// This method is used by the /api/metadata web script
		String jsonStr = "{}";

		if (this.nodeService.exists(nodeRef)) {
			JSONObject json = new JSONObject();

			try {
				json.put("nodeRef", nodeRef.toString());
				QName type = nodeService.getType(nodeRef);
				String typeString = type.toPrefixString(namespaceService);
				json.put("type", typeString);
				json.put("path", nodeService.getPath(nodeRef));
				json.put("displayPath", nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService));

				Status nodeStatus = this.nodeService.getNodeStatus(nodeRef);
				json.put("transactionId", nodeStatus.getDbTxnId());
				json.put("isDeleted", nodeStatus.isDeleted());

				extractProperties(nodeRef, json);
				extractAspects(nodeRef, json);
				extractPermissionInformation(nodeRef, json);
				extractVersionInformation(nodeRef, json);
				extractContentInformation(nodeRef, json, type);
				extractRulesInformation(nodeRef, json);
				extractWorkflowInformation(nodeRef, json);
				extractRenditionInformation(nodeRef, json);
				extractTagsInformation(nodeRef, json);
				extractLockInformation(nodeRef, json);

				json.put("webdav url",
						sysAdminParams.getAlfrescoProtocol() + "://" + sysAdminParams.getAlfrescoHost() + ":"
								+ sysAdminParams.getAlfrescoPort() + "/" + sysAdminParams.getAlfrescoContext()
								+ webDavService.getWebdavUrl(nodeRef));

				json.put("audits", getAudits(nodeRef, true));
				json.put("audit count", getAudits(nodeRef, false).size());

			} catch (JSONException error) {
				LOGGER.warn("JSON error creating node dump", error);
			}

			jsonStr = json.toString();
		}

		return new JsConsoleDump(nodeRef.toString(), jsonStr);
	}

	/**
	 * Extracts tags information of a node into the aggregate dump JSON object
	 * structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract tags
	 *                information
	 * @param json    the JSON object structure into which to add tags information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractTagsInformation(final NodeRef nodeRef, JSONObject json) throws JSONException {
		List<String> tags = tagService.getTags(nodeRef);
		json.put("tags count", tags.size());

		JSONArray tagsJson = new JSONArray();
		for (String tag : tags) {
			tagsJson.put(tag);

		}
		json.put("tags", tagsJson);
	}

	/**
	 * Extracts workflow counts information of a node into the aggregate dump JSON
	 * object structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract workflow
	 *                counts information
	 * @param json    the JSON object structure into which to add workflow counts
	 *                information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractWorkflowInformation(final NodeRef nodeRef, JSONObject json) throws JSONException {
		json.put("workflows (active/completed)", workflowService.getWorkflowsForContent(nodeRef, true).size() + " / "
				+ workflowService.getWorkflowsForContent(nodeRef, false).size());
	}

	/**
	 * Extracts {@link ContentModel#PROP_CONTENT content} information of a node into
	 * the aggregate dump JSON object structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract content
	 *                information
	 * @param json    the JSON object structure into which to add content
	 *                information
	 * @param type    the qualified type of the node
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractContentInformation(final NodeRef nodeRef, JSONObject json, QName type) throws JSONException {
		if (dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT)) {
			ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			if (contentReader != null) {
				json.put("content encoding", contentReader.getEncoding());
				json.put("content mimetype", contentReader.getMimetype());
				json.put("content size", byteCountToDisplaySize(contentReader.getSize()));
				json.put("content locale", contentReader.getLocale());
				json.put("content lastModified", new Date(contentReader.getLastModified()));
				json.put("content url", contentReader.getContentUrl());
			}
		}
	}

	// similar result to commons-io FileUtils#byteCountToDisplaySize(BigInteger)
	/**
	 * Byte count to display size.
	 *
	 * @param size the size
	 * @return the string
	 */
	// implemented here to avoid hard dependency (flagged by extension inspector)
	private static String byteCountToDisplaySize(long size) {
		String displaySize = String.valueOf(size) + " bytes";
		if (size > 1024) {
			BigInteger bis = BigInteger.valueOf(size);
			BigInteger unitStep = BigInteger.valueOf(2 ^ 10);
			BigInteger unitDivisor = BigInteger.valueOf(2 ^ 60);
			String[] unitSuffixes = { " EB", " PB", " TB", " GB", " MB", " KB" };
			for (String unitSuffix : unitSuffixes) {
				if (bis.compareTo(unitDivisor) > 0) {
					displaySize = String.valueOf(bis.divide(unitDivisor)) + unitSuffix;
					break;
				}
				unitDivisor = unitDivisor.divide(unitStep);
			}
		}
		return displaySize;
	}

	/**
	 * Extracts lock status information of a node into the aggregate dump JSON
	 * object structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract lock
	 *                information
	 * @param json    the JSON object structure into which to add lock information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	public void extractLockInformation(NodeRef nodeRef, JSONObject json) throws JSONException {
		Set<QName> nodeAspects = this.nodeService.getAspects(nodeRef);

		if (nodeAspects.contains(ContentModel.ASPECT_LOCKABLE)) {
			LockState lockState = this.lockService.getLockState(nodeRef);
			json.put("lock Status", this.lockService.getLockStatus(nodeRef).toString());
			json.put("lock Type", this.lockService.getLockType(nodeRef));
			json.put("lock Owner", lockState.getOwner());
			json.put("lock ExpireDate", lockState.getExpires());
			json.put("lock LifeTime", lockState.getLifetime());
			json.put("lock additional info", lockState.getAdditionalInfo());
		} else {
			json.put("lock Status", "-");

		}
	}

	/**
	 * Extracts version information of a node into the aggregate dump JSON object
	 * structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract version
	 *                information
	 * @param json    the JSON object structure into which to add version
	 *                information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractVersionInformation(final NodeRef nodeRef, JSONObject json) throws JSONException {
		json.put("isAVersion", versionService.isAVersion(nodeRef));
		json.put("isVersioned", versionService.isVersioned(nodeRef));

		VersionHistory versionHistory = versionService.getVersionHistory(nodeRef);
		if (versionHistory != null) {
			json.put("version count", versionHistory.getAllVersions().size());
			Collection<Version> allVersions = versionHistory.getAllVersions();
			List<String> tooltipFragments = new ArrayList<>(allVersions.size());
			for (Version version : allVersions) {
				tooltipFragments.add(version.getVersionProperties().toString());
			}
			json.put("version count tooltip", tooltipFragments);
		} else {
			json.put("version count", "0");
		}
	}

	/**
	 * Extracts properties information of a node into the aggregate dump JSON object
	 * structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract properties
	 *                information
	 * @param json    the JSON object structure into which to add properties
	 *                information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractProperties(final NodeRef nodeRef, JSONObject json) throws JSONException {
		// add properties
		Map<QName, Serializable> nodeProperties = this.nodeService.getProperties(nodeRef);

		Map<String, Serializable> nodePropertiesShortQNames = new TreeMap<String, Serializable>();
		for (Entry<QName, Serializable> entry : nodeProperties.entrySet()) {
			QName qn = entry.getKey();
			Serializable value = entry.getValue();
			try {
				nodePropertiesShortQNames.put(qn.toPrefixString(namespaceService), value);
			} catch (NamespaceException ne) {
				LOGGER.debug("Ignoring property '{}' as it's namespace is not registered", qn);
			}
		}

		json.put("properties", nodePropertiesShortQNames);
	}

	/**
	 * Extracts aspects information of a node into the aggregate dump JSON object
	 * structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract aspects
	 *                information
	 * @param json    the JSON object structure into which to add aspects
	 *                information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractAspects(final NodeRef nodeRef, JSONObject json) throws JSONException {
		// add aspects as an array
		Set<QName> nodeAspects = this.nodeService.getAspects(nodeRef);
		Set<String> nodeAspectsShortQNames = new LinkedHashSet<>(nodeAspects.size());
		for (QName nextLongQName : nodeAspects) {
			nodeAspectsShortQNames.add(nextLongQName.toPrefixString(namespaceService));
		}
		json.put("aspects", nodeAspectsShortQNames);
	}

	/**
	 * Extracts permission information of a node into the aggregate dump JSON object
	 * structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract permission
	 *                information
	 * @param json    the JSON object structure into which to add permission
	 *                information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractPermissionInformation(final NodeRef nodeRef, JSONObject json) throws JSONException {
		json.put("inheritPermissions", permissionService.getInheritParentPermissions(nodeRef));

		JSONArray permissionJson = new JSONArray();

		Set<AccessPermission> permissions = permissionService.getAllSetPermissions(nodeRef);
		for (AccessPermission accessPermission : permissions) {
			JSONObject permission = new JSONObject();
			permission.put("authority", accessPermission.getAuthority());
			permission.put("authorityType", accessPermission.getAuthorityType());
			permission.put("accessStatus", accessPermission.getAccessStatus());
			permission.put("permission", accessPermission.getPermission());
			permission.put("directly", accessPermission.isSetDirectly());
			permissionJson.put(permission);
		}

		json.put("permissions", permissionJson);
	}

	/**
	 * Extracts renditions information of a node into the aggregate dump JSON object
	 * structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract renditions
	 *                information
	 * @param json    the JSON object structure into which to add renditions
	 *                information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractRenditionInformation(final NodeRef nodeRef, JSONObject json) throws JSONException {
		// cannot use RenditionService or RenditionService2
		// not consistently available / supported across the various ACS versions
		List<ChildAssociationRef> renditions = nodeService.getChildAssocs(nodeRef, RenditionModel.ASSOC_RENDITION,
				RegexQNamePattern.MATCH_ALL);
		json.put("renditions count", renditions.size());

		JSONArray renditionsJson = new JSONArray();
		for (ChildAssociationRef rendition : renditions) {
			JSONObject rendtionJson = new JSONObject();
			rendtionJson.put("typeName", rendition.getTypeQName().toPrefixString(namespaceService));
			rendtionJson.put("qName", rendition.getQName().toPrefixString(namespaceService));
			rendtionJson.put("childType",
					nodeService.getType(rendition.getChildRef()).toPrefixString(namespaceService));
			renditionsJson.put(rendtionJson);
		}
		json.put("renditions", renditionsJson);
	}

	/**
	 * Extracts rules information of a node into the aggregate dump JSON object
	 * structure.
	 * 
	 * @param nodeRef the reference to the node from which to extract rules
	 *                information
	 * @param json    the JSON object structure into which to add rules information
	 * @throws JSONException if an error occurs filling in the JSON object structure
	 */
	private void extractRulesInformation(final NodeRef nodeRef, JSONObject json) throws JSONException {
		List<Rule> rulesLocal = ruleService.getRules(nodeRef, false);
		json.put("rules local ", rulesLocal.size());

		List<Rule> rules = ruleService.getRules(nodeRef, true);
		json.put("rules inherited ", rules.size() - rulesLocal.size());
		JSONArray rulesJson = new JSONArray();

		for (Rule rule : rules) {
			JSONObject ruleJson = new JSONObject();
			ruleJson.put("title", rule.getTitle());
			ruleJson.put("description", rule.getDescription());
			ruleJson.put("asynchronous", rule.getExecuteAsynchronously());
			ruleJson.put("disabled", rule.getRuleDisabled());
			ruleJson.put("ruleNode", rule.getNodeRef());
			ruleJson.put("ruleTypes", rule.getRuleTypes().toString());
			ruleJson.put("action", rule.getAction().getTitle());
			ruleJson.put("inherit", rule.isAppliedToChildren());
			NodeRef owningNodeRef = ruleService.getOwningNodeRef(rule);
			ruleJson.put("owningNodeRef", owningNodeRef.toString());
			rulesJson.put(ruleJson);
		}

		json.put("rules", rulesJson);
	}

	/**
	 * Extracts the node reference representing a particular value object, which can
	 * be either a {@link ScriptNode}, {@link NodeRef regular node reference} or a
	 * textual value.
	 * 
	 * @param value the value from which to extract the node reference
	 * @return the node reference
	 */
	private NodeRef extractNodeRef(Object value) {
		final NodeRef nodeRef;
		if (value instanceof ScriptNode) {
			nodeRef = ((ScriptNode) value).getNodeRef();
		} else if (value instanceof NodeRef) {
			nodeRef = (NodeRef) value;
		} else if (value instanceof String) {
			nodeRef = new NodeRef((String) value);
		} else {
			throw new IllegalArgumentException(
					"value of type " + value.getClass().getSimpleName() + " is not supported for dump");
		}
		return nodeRef;
	}

	/**
	 * Gets the audits.
	 *
	 * @param nodeRef the node ref
	 * @param limited the limited
	 * @return the audits
	 */
	private Collection<Map<String, Object>> getAudits(NodeRef nodeRef, final boolean limited) {
		// Execute the query
		AuditQueryParameters params = new AuditQueryParameters();
		params.setForward(false);
		params.addSearchKey(null, nodeRef.toString());

		final List<Map<String, Object>> entries = new ArrayList<>();
		AuditQueryCallback callback = new AuditQueryCallback() {

			/**
			 * 
			 * {@inheritDoc}
			 */
			@Override
			public boolean valuesRequired() {
				if (limited) {
					return true;
				} else {
					return false;
				}
			}

			/**
			 * 
			 * {@inheritDoc}
			 */
			@Override
			public boolean handleAuditEntryError(Long entryId, String errorMsg, Throwable error) {
				return true;
			}

			/**
			 * 
			 * {@inheritDoc}
			 */
			@Override
			public boolean handleAuditEntry(Long entryId, String applicationName, String user, long time,
					Map<String, Serializable> values) {

				Map<String, Object> entry = new HashMap<>(11);
				if (limited) {

					entry.put(JavascriptConsoleScriptObject.JSON_KEY_ENTRY_ID, entryId);
					entry.put(JavascriptConsoleScriptObject.JSON_KEY_ENTRY_APPLICATION, applicationName);
					if (user != null) {
						entry.put(JavascriptConsoleScriptObject.JSON_KEY_ENTRY_USER, user);
					}
					entry.put(JavascriptConsoleScriptObject.JSON_KEY_ENTRY_TIME, new Date(time));
					if (values != null) {
						// Convert values to Strings
						Map<String, String> valueStrings = new HashMap<>(values.size() * 2);
						for (Map.Entry<String, Serializable> mapEntry : values.entrySet()) {
							String key = mapEntry.getKey();
							Serializable value = mapEntry.getValue();
							try {
								String valueString = DefaultTypeConverter.INSTANCE.convert(String.class, value);
								valueStrings.put(key, valueString);
							} catch (TypeConversionException e) {
								valueStrings.put(key, value.toString());
							}

						}
						entry.put(JavascriptConsoleScriptObject.JSON_KEY_ENTRY_VALUES, valueStrings);
					}
					entries.add(entry);
					return true;
				} else {
					entries.add(entry);
					return true;
				}
			}
		};
		int limit;
		if (limited) {
			limit = 5;
		} else {
			limit = 100;
		}
		auditService.auditQuery(callback, params, limit);
		return entries;
	}

	/**
	 * Sets the node service.
	 *
	 * @param nodeService the new node service
	 */
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * Sets the permission service.
	 *
	 * @param permissionService the new permission service
	 */
	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	/**
	 * Sets the namespace service.
	 *
	 * @param namespaceService the new namespace service
	 */
	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	/**
	 * Sets the version service.
	 *
	 * @param versionService the new version service
	 */
	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

	/**
	 * Sets the content service.
	 *
	 * @param contentService the new content service
	 */
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	/**
	 * Sets the dictionary service.
	 *
	 * @param dictionaryService the new dictionary service
	 */
	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	/**
	 * Sets the rule service.
	 *
	 * @param ruleService the new rule service
	 */
	public void setRuleService(RuleService ruleService) {
		this.ruleService = ruleService;
	}

	/**
	 * Sets the workflow service.
	 *
	 * @param workflowService the new workflow service
	 */
	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	/**
	 * Sets the tag service.
	 *
	 * @param tagService the new tag service
	 */
	public void setTagService(TaggingService tagService) {
		this.tagService = tagService;
	}

	/**
	 * Sets the web dav service.
	 *
	 * @param webDavService the new web dav service
	 */
	public void setWebDavService(WebDavService webDavService) {
		this.webDavService = webDavService;
	}

	/**
	 * Sets the audit service.
	 *
	 * @param auditService the new audit service
	 */
	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	/**
	 * Sets the sys admin params.
	 *
	 * @param sysAdminParams the new sys admin params
	 */
	public void setSysAdminParams(SysAdminParams sysAdminParams) {
		this.sysAdminParams = sysAdminParams;
	}

	/**
	 * Sets the dump counter.
	 *
	 * @param dumpCounter the new dump counter
	 */
	public void setDumpCounter(AtomicInteger dumpCounter) {
		this.dumpCounter = dumpCounter;
	}

	/**
	 * Sets the dump limit.
	 *
	 * @param dumpLimit the new dump limit
	 */
	public void setDumpLimit(int dumpLimit) {
		this.dumpLimit = dumpLimit;
	}

	/**
	 * Sets the lock service.
	 *
	 * @param lockService the new lock service
	 */
	public void setLockService(LockService lockService) {
		this.lockService = lockService;
	}

}
