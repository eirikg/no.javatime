package no.javatime.lang.design.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;

import no.javatime.lang.model.Element;
import no.javatime.lang.model.Field;
import no.javatime.lang.model.Member;
import no.javatime.lang.model.Model;
import no.javatime.lang.model.ModelElementType;
import no.javatime.lang.model.PackageDeclaration;

public class ModelServices {
    
	public EObject openTextEditor(EObject any) {
		if (any != null && any.eResource() instanceof XtextResource
				&& any.eResource().getURI() != null) {

			String fileURI = any.eResource().getURI().toPlatformString(true);
			IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(new Path(fileURI));
			if (workspaceFile != null) {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				try {
					IEditorPart openEditor = IDE.openEditor(page,
							workspaceFile,
							"no.javatime.lang.Model",
							true);
					if (openEditor instanceof AbstractTextEditor) {
						ICompositeNode node = NodeModelUtils
								.findActualNodeFor(any);
						if (node != null) {
							int offset = node.getOffset();
							int length = node.getTotalEndOffset() - offset;
							((AbstractTextEditor) openEditor).selectAndReveal(
									offset, length);
							System.out
									.println("ModelServices.openTextEditor()");
						}
					}
					// editorInput.
				} catch (PartInitException e) {
					// Put your exception handler here if you wish to.
				}
			}
		}
		System.out.println(any);
		return any;
	}

	static int st = 0;

	/**
	 * Get all model element types referenced from field members of the specified
	 * element
	 * <p>
	 * An {@link Element} is a super type of a {@link PackageDeclaration} and
	 * a {@link ModelElementType} 
	 *  
	 * @param element A package declaration or a model element type
	 * @return
	 */
	public Collection<EObject> getModelElementTypes(Element element) {

		List<EObject> result = new ArrayList<>();		

		System.out.println(++st + "--- Start any info: " + element);
		System.out.println(st + " eContainer(): " + element.eContainer());		
		System.out.println(st + " eClass(): " + element.eClass());		
		// empty System.out.println(st + " eAllContents(): " + any.eAllContents());		
		System.out.println(st + " eContainmentFeature(): " + element.eContainmentFeature());		
		System.out.println(st + " eContents(): " + element.eContents());		
		System.out.println(st + "--- End any info: " + element);
		// Exclude package declarations
		if (element instanceof ModelElementType) {
			ModelElementType elemType = (ModelElementType) element;
			System.out.println(st + " " + elemType);
			// getAllElements(element);
			// Get all field and operation members
			List<Member> members = elemType.getMembers();
			for (Member member : members) {
				System.out.println(st + " Member " + member);
				// Exclude operations
				if (member instanceof Field) {
					// Find the declarated model element type of this field reference
					Field fieldMember = (Field) member;
					JvmTypeReference typeRef = fieldMember.getMemberType();
					System.out.println(st + " JvmTypeReference " + typeRef);
					JvmType type = typeRef.getType();
					System.out.println(st + " JvmType " + type);
					// Get all elements within the package of element type
					// EList<EObject> elements = elemType.eContainer().eContents();
					Collection<EObject> elements = getAllElements(element);
					for (EObject elem : elements) {
						if (elem instanceof ModelElementType) {
							EObject p = elem.eContainer();
							if (p instanceof PackageDeclaration) {
								String packageName = ((PackageDeclaration) p).getName();
								String qualifiedName = packageName + "." + ((ModelElementType) elem).getName(); 
								System.out.println(st + " Elem name == type name " + qualifiedName + " == " + type.getQualifiedName());
								if (type.getQualifiedName().equals(qualifiedName)) {
									result.add(elem);							
								}							
							}
						}
					}
				}
			}
		}
		return result;
	}

	public Collection<EObject> getAllElements(Element element) {
		
		Collection<EObject> elementTypes = new ArrayList<>();
		
		if (element instanceof ModelElementType) {
			ModelElementType elemType = (ModelElementType) element;
			// Get the package of this model element type and then get the model
			EObject model = elemType.eContainer().eContainer();
			System.out.println("Model " + model);
			if (model instanceof Model) {
				List<Element> elements = ((Model) model).getElements();
				for (Element elem : elements) {
					if (elem instanceof PackageDeclaration) {
						List<Element> packageElements = ((PackageDeclaration) elem).getPackageElements();
						for (Element mElem : packageElements) {
							if (mElem instanceof ModelElementType) {
								System.out.println("Model element type" + mElem);
								elementTypes.add(mElem);								
							}
						}
					}
				}
			}
		}
		// else if element is package declaration
		return elementTypes;
	}
	
	public boolean isValid(EObject any) {
		System.out.println(st++ + " Valid: " + any);		
		return true;
	}
}
