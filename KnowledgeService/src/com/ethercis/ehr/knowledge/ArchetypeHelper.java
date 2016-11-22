package com.ethercis.ehr.knowledge;

import org.openehr.am.archetype.Archetype;
import org.openehr.am.archetype.assertion.Assertion;
import org.openehr.am.archetype.assertion.ExpressionBinaryOperator;
import org.openehr.am.archetype.constraintmodel.*;
import se.acode.openehr.parser.ParseException;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class ArchetypeHelper {
	public CObject getCObject(ArchetypeInternalRef ref) throws ParseException {
		String path = ref.getTargetPath();
		StringTokenizer tokenizer = new StringTokenizer(path.substring(1), "/");
		CObject ans = at.getDefinition();
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int nameLength = token.indexOf('[');
			if (nameLength > 0) {
				String attrName = token.substring(0, nameLength);
				String nodeId = token.substring(nameLength + 1,
						token.indexOf(']'));
				if (!(ans instanceof CComplexObject)) {
					throw new ParseException("There isn't any attribute of "
							+ ans.getNodeId());
				}
				CAttribute attr = ((CComplexObject) ans).getAttribute(attrName);
				if (attr == null) {
					throw new ParseException("There is no attribute named "
							+ attrName + " in node " + ans.getNodeId());
				}
				CObject next = null;
				for (CObject cobj : attr.getChildren()) {
					if (nodeId.equals(cobj.getNodeId())) {
						next = cobj;
						break;
					}
				}
				if (next == null) {
					throw new ParseException("There is no nodeId +'" + nodeId
							+ "' in attribute '" + attrName + "' of "
							+ ans.getNodeId());
				}
				ans = next;
			} else {
				throw new ParseException("targetPath is invalid: " + path);
			}
		}
		return ans;
	}

	public Archetype archetypeFromSlot(ArchetypeSlot slot) throws Exception {
		List<Archetype> at = archetypesFromSlot(slot);
		if (at != null && at.size() > 0) {
			return at.get(0);
		} else {
			return null;
		}
	}

	public List<Archetype> archetypesFromSlot(ArchetypeSlot slot)
			throws Exception {
		Pattern[] patterns = fromSlot(slot);
		return ar.retrieveArchetypes(patterns[0], patterns[1]);
	}

	public ArchetypeHelper(Archetype at, I_KnowledgeCache ar) {
		this.at = at;
		this.ar = ar;
	}

	private Pattern[] fromSlot(ArchetypeSlot slot) throws ParseException {
		if (slot.getNodeId() == null) {
			throw new ParseException(
					"ArchetypeSlot doens't have nodeId(nodeId is null)!");
		}
		Pattern includes = null;
		Pattern excludes = null;
		if (slot.getIncludes() != null && slot.getIncludes().size() > 0) {
			for (Assertion a : slot.getIncludes()) {
				if (a.getExpression() instanceof ExpressionBinaryOperator) {
					ExpressionBinaryOperator bi = (ExpressionBinaryOperator) a
							.getExpression();
					String regex = bi.getRightOperand().toString();
					regex = regex.substring(1, regex.length() - 1);
					includes = Pattern.compile(regex);
				} else {
					throw new ParseException("One of assertions in "
							+ slot.getNodeId() + ".includes is invalid!");
				}
			}
		}

		if (slot.getExcludes() != null && slot.getExcludes().size() > 0) {
			for (Assertion a : slot.getExcludes()) {
				if (a.getExpression() instanceof ExpressionBinaryOperator) {
					ExpressionBinaryOperator bi = (ExpressionBinaryOperator) a
							.getExpression();
					String regex = bi.getRightOperand().toString();
					regex = regex.substring(1, regex.length() - 1);
					excludes = Pattern.compile(regex);
				} else {
					throw new ParseException("One of assertions in "
							+ slot.getNodeId() + ".excludes is invalid!");
				}
			}
		}

		return new Pattern[] { includes, excludes };
	}

	private Archetype at;
	private I_KnowledgeCache ar;
}
