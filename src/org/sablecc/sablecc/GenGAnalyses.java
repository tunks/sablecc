/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This file is part of SableCC.                             *
 * See the file "LICENSE" for copyright information and the  *
 * terms and conditions for copying, distribution and        *
 * modification of SableCC.                                  *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package org.sablecc.sablecc;

import org.sablecc.sablecc.analysis.*;
import org.sablecc.sablecc.node.*;
import java.util.*;
import java.io.*;

/**
 * Like {@link GenAnalyses}, but generate a visitor with generic
 * return type.
 * <p>
 * This is a quick and dirty copy-clone of {@link GenAnalyses}. 
 *  
 * @author anton (reengineered from decompiled source by Paresh Paradkar)
 *
 */
public class GenGAnalyses extends DepthFirstAdapter
{
  private MacroExpander macros;
  private ResolveAstIds ast_ids;
  private File pkgDir;
  private String pkgName;
  private List elemList;
  private List altList = new TypedLinkedList(AltInfoCast.instance);
  private List tokenList = new TypedLinkedList(StringCast.instance);
  private String mainProduction;

  ElemInfo info;
  //    final GenAnalyses instance = this;

  public GenGAnalyses(ResolveAstIds ast_ids)
  {
    this.ast_ids = ast_ids;
    try
    {
      macros = new MacroExpander(
                 new InputStreamReader(
                   getClass().getResourceAsStream("genanalyses.txt")));
    }
    catch(IOException e)
    {
      throw new RuntimeException("unable to open genanalyses.txt.");
    }

    pkgDir = new File(ast_ids.astIds.pkgDir, "analysis");
    pkgName = ast_ids.astIds.pkgName.equals("") ? "analysis" : ast_ids.astIds.pkgName + ".analysis";

    if(!pkgDir.exists())
    {
      if(!pkgDir.mkdir())
      {
        throw new RuntimeException("Unable to create " + pkgDir.getAbsolutePath());
      }
    }
  }

  public void inAAstProd(AAstProd node)
  {
    if(mainProduction == null)
    {
      mainProduction = (String) ast_ids.ast_names.get(node);
    }
  }

  public void inATokenDef(ATokenDef node)
  {
    tokenList.add(ast_ids.astIds.names.get(node));
  }

  public void inAAstAlt(AAstAlt node)
  {
    elemList = new TypedLinkedList(ElemInfoCast.instance);
  }

  public void caseAProductions(AProductions node)
  {}

  public void inAElem(AElem node)
  {
    info = new ElemInfo();

    info.name = (String) ast_ids.ast_names.get(node);
    info.type = (String) ast_ids.ast_elemTypes.get(node);
    info.operator = ElemInfo.NONE;

    if(node.getUnOp() != null)
    {
      node.getUnOp().apply(new DepthFirstAdapter()
                           {

                             public void caseAStarUnOp(AStarUnOp node)
                             {
                               info.operator = ElemInfo.STAR;
                             }

                             public void caseAQMarkUnOp(AQMarkUnOp node)
                             {
                               info.operator = ElemInfo.QMARK;
                             }

                             public void caseAPlusUnOp(APlusUnOp node)
                             {
                               info.operator = ElemInfo.PLUS;
                             }
                           }
                          );
    }

    elemList.add(info);
    info = null;
  }

  public void outAAstAlt(AAstAlt node)
  {
    AltInfo info = new AltInfo();

    info.name = (String) ast_ids.ast_names.get(node);
    info.elems.addAll(elemList);
    elemList = null;

    altList.add(info);
  }

  public void outStart(Start node)
  {
    createAnalysis();
    createAnalysisAdapter();

    if(mainProduction != null)
    {
      createDepthFirstAdapter();
      createReversedDepthFirstAdapter();
    }
  }

  public void createAnalysis()
  {
    BufferedWriter file;

    try
    {
      file = new BufferedWriter(
               new FileWriter(
                 new File(pkgDir, "GenericAnalysis.java")));
    }
    catch(IOException e)
    {
      throw new RuntimeException("Unable to create " + new File(pkgDir, "GenericAnalysis.java").getAbsolutePath());
    }

    try
    {
      macros.apply(file, "GenAnalysisHeader", new String[] {pkgName,
                   ast_ids.astIds.pkgName.equals("") ? "node" : ast_ids.astIds.pkgName + ".node"});

      if(mainProduction != null)
      {
        macros.apply(file, "GenAnalysisStart", null);

        for(Iterator i = altList.iterator(); i.hasNext();)
        {
          AltInfo info = (AltInfo) i.next();

          macros.apply(file, "GenAnalysisBody",
                       new String[] {info.name});
        }

        file.newLine();
      }

      for(Iterator i = tokenList.iterator(); i.hasNext();)
      {
        macros.apply(file, "GenAnalysisBody",
                     new String[] {(String) i.next()});
      }

      macros.apply(file, "GenAnalysisTail", null);
    }
    catch(IOException e)
    {
      throw new RuntimeException("An error occured while writing to " +
                                 new File(pkgDir, "GenericAnalysis.java").getAbsolutePath());
    }

    try
    {
      file.close();
    }
    catch(IOException e)
    {}
  }

  public void createAnalysisAdapter()
  {
    BufferedWriter file;

    try
    {
      file = new BufferedWriter(
               new FileWriter(
                 new File(pkgDir, "GenericAnalysisAdapter.java")));
    }
    catch(IOException e)
    {
      throw new RuntimeException("Unable to create " + new File(pkgDir, "GenericAnalysisAdapter.java").getAbsolutePath());
    }

    try
    {
      macros.apply(file, "GenAnalysisAdapterHeader", new String[] {pkgName,
                   ast_ids.astIds.pkgName.equals("") ? "node" : ast_ids.astIds.pkgName + ".node"});

      if(mainProduction != null)
      {
        macros.apply(file, "GenAnalysisAdapterStart", null);

        for(Iterator i = altList.iterator(); i.hasNext();)
        {
          AltInfo info = (AltInfo) i.next();

          macros.apply(file, "GenAnalysisAdapterBody",
                       new String[] {info.name});
        }
      }

      for(Iterator i = tokenList.iterator(); i.hasNext();)
      {
        macros.apply(file, "GenAnalysisAdapterBody",
                     new String[] {(String) i.next()});
      }

      macros.apply(file, "GenAnalysisAdapterTail", null);
    }
    catch(IOException e)
    {
      throw new RuntimeException("An error occured while writing to " +
                                 new File(pkgDir, "GenericAnalysisAdapter.java").getAbsolutePath());
    }

    try
    {
      file.close();
    }
    catch(IOException e)
    {}
  }

  public void createDepthFirstAdapter()
  {
    BufferedWriter file;

    try
    {
      file = new BufferedWriter(
               new FileWriter(
                 new File(pkgDir, "GenericDepthFirstAdapter.java")));
    }
    catch(IOException e)
    {
      throw new RuntimeException("Unable to create " + new File(pkgDir, "GenericDepthFirstAdapter.java").getAbsolutePath());
    }

    try
    {
      macros.apply(file, "GenDepthFirstAdapterHeader", new String[] {pkgName,
                   ast_ids.astIds.pkgName.equals("") ? "node" : ast_ids.astIds.pkgName + ".node",
                   mainProduction});

      for(Iterator i = altList.iterator(); i.hasNext();)
      {
        AltInfo info = (AltInfo) i.next();

        macros.apply(file, "GenDepthFirstAdapterInOut",
                     new String[] {info.name});

        macros.apply(file, "GenDepthFirstAdapterCaseHeader",
                     new String[] {info.name});

        for(Iterator j = info.elems.iterator(); j.hasNext();)
        {
          ElemInfo eInfo = (ElemInfo) j.next();

          switch(eInfo.operator)
          {
          case ElemInfo.QMARK:
          case ElemInfo.NONE:
            {
              macros.apply(file, "GenDepthFirstAdapterCaseBodyNode",
                           new String[] {eInfo.name});
            }
            break;
          case ElemInfo.STAR:
          case ElemInfo.PLUS:
            {
              macros.apply(file, "GenDepthFirstAdapterCaseBodyList",
                           new String[] {eInfo.name, eInfo.type});
            }
            break;
          }
        }

        macros.apply(file, "GenDepthFirstAdapterCaseTail",
                     new String[] {info.name});

      }

      macros.apply(file, "GenDepthFirstAdapterTail", null);
    }
    catch(IOException e)
    {
      throw new RuntimeException("An error occured while writing to " +
                                 new File(pkgDir, "DepthFirstAdapter.java").getAbsolutePath());
    }

    try
    {
      file.close();
    }
    catch(IOException e)
    {}
  }

  public void createReversedDepthFirstAdapter()
  {
    BufferedWriter file;

    try
    {
      file = new BufferedWriter(
               new FileWriter(
                 new File(pkgDir, "GenericReversedDepthFirstAdapter.java")));
    }
    catch(IOException e)
    {
      throw new RuntimeException("Unable to create " + new File(pkgDir, "GenericReversedDepthFirstAdapter.java").getAbsolutePath());
    }

    try
    {
      macros.apply(file, "GenReversedDepthFirstAdapterHeader", new String[] {pkgName,
                   ast_ids.astIds.pkgName.equals("") ? "node" : ast_ids.astIds.pkgName + ".node",
                   mainProduction});

      for(Iterator i = altList.iterator(); i.hasNext();)
      {
        AltInfo info = (AltInfo) i.next();

        macros.apply(file, "GenDepthFirstAdapterInOut",
                     new String[] {info.name});

        macros.apply(file, "GenDepthFirstAdapterCaseHeader",
                     new String[] {info.name});

        for(ListIterator j = info.elems.listIterator(info.elems.size()); j.hasPrevious();)
        {
          ElemInfo eInfo = (ElemInfo) j.previous();

          switch(eInfo.operator)
          {
          case ElemInfo.QMARK:
          case ElemInfo.NONE:
            {
              macros.apply(file, "GenDepthFirstAdapterCaseBodyNode",
                           new String[] {eInfo.name});
            }
            break;
          case ElemInfo.STAR:
          case ElemInfo.PLUS:
            {
              macros.apply(file, "GenReversedDepthFirstAdapterCaseBodyList",
                           new String[] {eInfo.name, eInfo.type});
            }
            break;
          }
        }

        macros.apply(file, "GenDepthFirstAdapterCaseTail",
                     new String[] {info.name});

      }

      macros.apply(file, "GenDepthFirstAdapterTail", null);
    }
    catch(IOException e)
    {
      throw new RuntimeException("An error occured while writing to " +
                                 new File(pkgDir, "GenReversedDepthFirstAdapter.java").getAbsolutePath());
    }

    try
    {
      file.close();
    }
    catch(IOException e)
    {}
  }

  private static class ElemInfo
  {
    final static int NONE = 0;
    final static int STAR = 1;
    final static int QMARK = 2;
    final static int PLUS = 3;

    String name;
    String type;
    int operator;
  }

  private static class ElemInfoCast implements Cast
  {
    final static ElemInfoCast instance = new ElemInfoCast();

    private ElemInfoCast()
    {}

    public    Object cast(Object o)
    {
      return (ElemInfo) o;
    }
  }

  private static class AltInfo
  {
    String name;
    final List elems = new TypedLinkedList(ElemInfoCast.instance);
  }

  private static class AltInfoCast implements Cast
  {
    final static AltInfoCast instance = new AltInfoCast();

    private AltInfoCast()
    {}

    public    Object cast(Object o)
    {
      return (AltInfo) o;
    }
  }
}
