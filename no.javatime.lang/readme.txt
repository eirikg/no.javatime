Do this from the Model Explorer

A. Create a Sirius modeling bundle project with Xtetxt (.diagram):

1. create a bundle project (not a plug-in) and add to import:
	no.javatime.core.model.annotations
	com.google.inject 
packages
2. Right click bundle and select  Configure | convert to modeling project
3. Right click bundle and select  Configure | add Xtext nature
4. Activate workspace bundles
5. File | New | Example EMF Model Creation Wizard | Model Model
6. Add some packages and model element types to the created jtl file
7. Create a viewpoint and a representation
8. Open the diagram

B. Create a specification project (.design)

1. New  Viewpoint Specification Project
2. Specification project model name shoud have an .0desgn extension
	Second page in the "New  Viewpoint Specification Project" wizard
	The root element name of the project is the name before the .odesign extension name
3. From the root element create a viewpoint and in the properties view specify:
	An id and a name 
	jtl as the file extension which is the extension of the template file and of the xtext extension files
4. Select New Representation | Diagram Description and specify:
	A name and an id
	The base object: model.Model or model.Element (represents a package or a set of elements with a default package)
5. Open the diagram editor (created under bullet A) and the base objects (bullet B.4) should be visible