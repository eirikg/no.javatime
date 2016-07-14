package no.javatime.lang.design.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import no.javatime.lang.model.Element;
import no.javatime.lang.model.Field;
import no.javatime.lang.model.Member;
import no.javatime.lang.model.Model;
import no.javatime.lang.model.ModelElementType;
import no.javatime.lang.model.PackageDeclaration;
import no.javatime.lang.model.impl.PackageDeclarationImpl;

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
		print("Parameter", element, ++st);
		// Exclude package declarations
		if (element instanceof ModelElementType) {
			ModelElementType elemType = (ModelElementType) element;
			// getAllElements(element);
			// Get all field and operation members
			List<Member> members = elemType.getMembers();
			for (Member member : members) {
				// Exclude operations
				if (member instanceof Field) {
					print("Member of " + elemType.getName(), member, st);
					// Find the declarated model element type of this field reference
					Field fieldMember = (Field) member;
					JvmTypeReference typeRef = fieldMember.getMemberType();
					JvmType type = typeRef.getType();
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
			Model model = getModel(elemType);
			if (null != model) {
				List<Element> elements = model.getElements();
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
	
	/**
	 * Get the root model object based on an element
	 * 
	 * @param element Kind of element, model element type or package declaration 
	 * @return The model or null if the model could not be obtained
	 */
	public Model getModel(Element element) {
		
		Model model = null;
		EObject container = (null == element) ? element : element.eContainer();
		if (container instanceof PackageDeclaration) {
			// Element is type of Element or ModelElementType
			model = (Model) container.eContainer();							
		} else {
			// Element is type of PackageDeclaration or null
			model = (Model) container;							
		}
		return model;
	}

	public Collection<PackageDeclaration> getAllPackageElements(Element element) {
		
		Collection<PackageDeclaration> packages = new ArrayList<>();
		
			Model model = getModel(element);
			if (null != model) {
				List<Element> elements = model.getElements();
				for (Element elem : elements) {
					if (elem instanceof PackageDeclaration) {
						packages.add((PackageDeclaration) elem);								
					}
				}
		}
		// else if element is package declaration
		return packages;
	}
	
	/**
	 * Creates an unique package name based on location of the resource 
	 * of the specified package element and a unique number
	 * <p>
	 * Format of name: &ltlocation of package element&gt .newpackage &ltnumber of packages including the one to create&gt
	 *<p>
	 * Does not take into account duplicates for saved packages deleted from diagram but not from the model.
	 * E.g., create a new package, add some content, save it, delete it from the diagram, create a new package and save it
	 * The content added to first package will be lost after saving the second created package when the packages have the same name
	 *   
	 * @param packageElement The package object receiving the new name
	 * @return The name of the specified package element parameter
	 */
	public String getNewPackagename(PackageDeclaration packageElement) throws IOException {
		
		StringBuffer newPackageName = new StringBuffer();
		newPackageName.append(packageElement.eResource().getURI().segment(1));
		// packageElement.eResource().load(Collections.emptyMap());
		// Bundle bundle = FrameworkUtil.getBundle(packageElement.getClass());
		newPackageName.append(".newpackage");
		// The collection include the the package (the packageElement parameter object) to create
		Collection<PackageDeclaration> pds = getAllPackageElements(packageElement);
		// Create an unique package name
		int newPackageNo = 1;		
		boolean duplicate;
		do {
			duplicate = false;
			String tempPackageName = newPackageName.toString() + Integer.toString(newPackageNo); 
			for (PackageDeclaration pd : pds) {
				// A null package name is the new one to be named
				if (null != pd.getName() && pd.getName().equalsIgnoreCase(tempPackageName)) {
					duplicate = true;
					newPackageNo++;
					System.out.println(pd.getName() + " == " + tempPackageName + " duplicate = " + duplicate + " new size = " + newPackageNo);
					break;
				} 
			}
		} while (duplicate);
		newPackageName.append(Integer.toString(newPackageNo));
		return newPackageName.toString();
	}
	
	public boolean isValid(EObject any) {
		System.out.println(++st + " Valid: " + any);		
		return true;
	}
	
	static int st = 0;
	void print(String text, EObject element, int st) {

		if (element instanceof ModelElementType) {
			ModelElementType elemType = (ModelElementType) element;
			System.out.println(st + "--- Start Element: " + elemType.getName());
			System.out.println(" Internal function of element: " + text);		
			if (element.eContainer() instanceof PackageDeclaration) {
				PackageDeclaration pd = (PackageDeclaration) element.eContainer();
				System.out.println(" Package name (eContainer()): " + pd.getName());		
				System.out.println(" Model of package(eContainer()): " + pd.eContainer());		
			}
			System.out.println(" Element type name: " + elemType.getTypeName().getName());
			System.out.println(" eClass(): " + element.eClass().getName());		
			EReference ref = element.eContainmentFeature();
			System.out.println(" M:1 (contaimentFeature()): " + ref.getName());		
			System.out.println(st + "--- End Element: " + elemType.getName());
		} else if (element instanceof Field) {
			Field fieldType = (Field) element;
			System.out.println(st + "--- Start Field: " + fieldType.getName());
			System.out.println(" Internal function of field: " + text);		
			JvmTypeReference typeRef = fieldType.getMemberType();
			typeRef.getSimpleName();
			System.out.println(" JvmTypeReference Simple Name: " + typeRef.getSimpleName());
			System.out.println(" JvmTypeReference Qualified Name: " + typeRef.getQualifiedName());
			System.out.println(" JvmType Simple name: " + typeRef.getType().getSimpleName());
			System.out.println(st + "--- End Field: " + fieldType.getName());
		} else if (element instanceof PackageDeclaration) {
			PackageDeclaration packageDeclaration = (PackageDeclaration) element;
			System.out.println(st + "--- Start Element: " + packageDeclaration.getName());
			System.out.println(" Internal function of element: " + text);					
		}
	}	
}
