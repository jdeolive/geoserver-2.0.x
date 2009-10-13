package org.vfny.geoserver.issues;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.vfny.geoserver.issues.enums.Priority;
import org.vfny.geoserver.issues.enums.Resolution;

@Entity
@Table(name = "Issues")
public class Issue implements IIssue, Serializable {

    /** long serialVersionUID field */
    private static final long serialVersionUID = 465228904084847959L;
    private int id;
    private String description;
    private Resolution resolution;
    private Priority priority;
    private String targetString;
    private String mementoString;
    private String viewMementoString;
    private ReferencedEnvelope bounds;
    
    public Issue(){
        
    }
    
    public Issue(IIssue otherIssue){
        this.description = otherIssue.getDescription();
        this.resolution = otherIssue.getResolution();
        this.priority = otherIssue.getPriority();
        this.targetString = TargetWrapper.getStringFromTarget(otherIssue.getTarget());
        this.bounds = otherIssue.getBounds();
        XMLMemento memento = XMLMemento.createWriteRoot(MementoWrapper.ROOT_ID);
        otherIssue.save(memento);
        try {
            this.mementoString = memento.saveToString();
        } catch (IOException e) {
            this.mementoString = "";
        }
        XMLMemento viewMemento = XMLMemento.createWriteRoot(MementoWrapper.ROOT_ID);
        otherIssue.saveViewMemento(viewMemento);
        try {
            this.viewMementoString = viewMemento.saveToString();
        } catch (IOException e) {
            this.viewMementoString = "";
        }
        
    }

    /**
     * @return Returns the description.
     */
    @Column (length=1024)
    public String getDescription() {
        return this.description;
    }


    /**
     * Sets the description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    @Enumerated
    public Priority getPriority() {
        return this.priority;
    }

    
    public void setPriority( Priority priority ) {
        this.priority = priority;
    }

    @Enumerated
    public Resolution getResolution() {
        return resolution;
    }


    public void setResolution( Resolution newResolution ) {
        this.resolution = newResolution;
    }

    /**
     * @return Returns the id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int getId() {
        return this.id;
    }
    /**
     * @param id The id to set.
     */
    public void setId(Integer id) {
        this.id = id;
    }


    public void setBounds( ReferencedEnvelope bounds ) {
        this.bounds = bounds;
    }

    @Lob 
    public ReferencedEnvelope getBounds() {
        return this.bounds;
    }
 
    public void setMemento( IMemento memento ) {
        setMementoString(MementoWrapper.getStringFromMemento(memento));
    }

    @Transient
    public IMemento getMemento() {
        return MementoWrapper.getMementoFromString(getMementoString());
    }


    public void setViewMemento( IMemento memento ) {
        setViewMementoString(MementoWrapper.getStringFromMemento(memento));
    }

    @Transient
    public IMemento getViewMemento() {
        return MementoWrapper.getMementoFromString(getViewMementoString());
    }

    public void setTarget( Target target ) {
        setTargetString(TargetWrapper.getStringFromTarget(target));
    }

    @Transient
    public Target getTarget() {
        try{
            return TargetWrapper.getTargetFromString(getTargetString());
        }
        catch(Exception e){
            return null;
        }
    }
    
    @Lob
    public String getMementoString() {
        return mementoString;
    }

    public void setMementoString( String mementoString ) {
        this.mementoString = mementoString;
    }
    
    @Column (length=1024)
    public String getTargetString() {
        return targetString;
    }

    public void setTargetString( String targetString ) {
        this.targetString = targetString;
    }
    @Lob
    public String getViewMementoString() {
        return viewMementoString;
    }

    public void setViewMementoString( String viewMementoString ) {
        this.viewMementoString = viewMementoString;
    }

    public void save( IMemento memento ) {
        //do nothing, for the client to implement
    }

    public void saveViewMemento( IMemento memento ) {
    }

    
}
