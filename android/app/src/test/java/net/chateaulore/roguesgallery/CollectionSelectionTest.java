package net.chateaulore.roguesgallery;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class CollectionSelectionTest {
    private List<Cover> covers(int count) {
        List<Cover> result=new ArrayList<>();
        for(int i=0;i<count;i++)result.add(new Cover(String.valueOf(i),"Issue "+i,"https://example.com/"+i,""));
        return result;
    }
    private List<Cover> select(List<Cover> all,Set<String> excluded,int max,boolean random,List<String> old,boolean keep,int seed){
        return CollectionSelection.select(all,excluded,max,random,old,keep,new Random(seed));
    }
    @Test public void collectionBelowDefaultIsComplete(){
        assertEquals(188,select(covers(188),Collections.emptySet(),1000,true,Collections.emptyList(),false,1).size());
    }
    @Test public void randomSelectionUsesEntireCollectionAndFreshSeeds(){
        List<Cover> all=covers(2000);
        List<String> first=CollectionSelection.ids(select(all,Collections.emptySet(),1000,true,Collections.emptyList(),false,1));
        List<String> second=CollectionSelection.ids(select(all,Collections.emptySet(),1000,true,first,false,2));
        assertEquals(1000,first.size());assertNotEquals(first,second);
        assertTrue(first.stream().anyMatch(id->Integer.parseInt(id)>=1000));
    }
    @Test public void orderedSelectionSkipsExcludedAndDeduplicates(){
        List<Cover> all=covers(5);all.add(all.get(0));
        assertEquals(Arrays.asList("0","2","3"),CollectionSelection.ids(select(all,new HashSet<>(Arrays.asList("1")),3,false,Collections.emptyList(),false,1)));
    }
    @Test public void rememberedSelectionReplacesMissingAndExcluded(){
        assertEquals(Arrays.asList("0","2","4"),CollectionSelection.ids(select(covers(6),Collections.singleton("3"),3,false,Arrays.asList("2","3","4","99"),true,1)));
    }
    @Test public void playbackOrderDoesNotChangeMembershipOrCanonicalOrder(){
        List<Cover> selected=select(covers(20),Collections.emptySet(),10,true,Collections.emptyList(),false,1);
        List<Cover> shuffled=CollectionSelection.displayOrder(selected,true,new Random(2));
        assertEquals(new HashSet<>(CollectionSelection.ids(selected)),new HashSet<>(CollectionSelection.ids(shuffled)));
        int last=-1;for(Cover cover:CollectionSelection.displayOrder(selected,false,new Random(3))){int id=Integer.parseInt(cover.id);assertTrue(id>last);last=id;}
    }
    @Test public void variantsStayDistinct(){
        List<Cover> all=Arrays.asList(new Cover("1:","Regular","url",""),new Cover("1:2","Variant","url",""));
        assertEquals(2,select(all,Collections.emptySet(),1000,false,Collections.emptyList(),false,1).size());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsZeroMaximum(){
        select(covers(1),Collections.emptySet(),0,false,Collections.emptyList(),false,1);
    }
}
