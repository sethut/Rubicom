/*
 * Copyright 2019 Soongsil University. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __TASK_GRAPH_H__
#define __TASK_GRAPH_H__


#include <list>
#include <vector>
#include <iostream>
#include <iomanip>
#include <climits>
//#include <string>

enum vStat {UN_VISITED, VISITED, VRTX_NOT_FOUND};
enum eStat {DISCOVERY, BACK, CROSS, EDGE_UN_VISITED, EDGE_VISITED, EDGE_NOT_FOUND}; 

using namespace std;  

namespace task_graph
{

///////////////////////////////////////////////
// RosNode class
///////////////////////////////////////////////

  class RosNode {
    friend ostream& operator<<(ostream& fout, RosNode& n) {
      fout << "RosNode{ alive="
	   << n.alive << ", persist="
	   << n.persist << ", stage="
	   << n.stage << ", name="
	   << n.name << ", topic="
	   << n.topic << ", command="
	   << n.command << " }"; 
      return fout;
    }
  public:
    bool alive;
    bool persist;
    int stage;
    string name;
    string topic;
    string command;
    RosNode(): name(), topic(), command(), alive(false), persist(false), stage(-1) {}
  RosNode(string& n, string& t, string& c, bool p, int s): name(n), topic(t), command(c), alive(false), persist(p), stage(s) {}
    ~RosNode() {}
  };
  
///////////////////////////////////////////////
// Vertex class
///////////////////////////////////////////////

  class Vertex {                
    friend ostream& operator<<(ostream& fout, Vertex& v) {
      fout << "Vertex{ " << v.id << ", " << v.stat << ", " << v.node << " }"; 
      return fout;
    }      

  public:
    int id;       // id of vertex
    vStat stat;  // status of vertex (from a viewpoint of graph search)
    RosNode node; // ros node specific information

    Vertex(): id(-1), stat(UN_VISITED), node() {}
    Vertex(int i, vStat s, RosNode& n): id(i), stat(s), node(n) {}
    bool operator==(Vertex& v) { return (id == v.id); }
    bool operator!=(Vertex& v) { return (id != v.id); }
  };

  typedef list<Vertex> VtxList; 
  typedef list<Vertex>::iterator VtxItor;


///////////////////////////////////////////////
// RosTopic class
///////////////////////////////////////////////

  class RosTopic {
    friend ostream& operator<<(ostream& fout, RosTopic& t) {
      fout << "RosTopic{ " << t.name << " }"; 
      return fout;
    }
  public:
    string name;
    RosTopic(): name() {}
    RosTopic(string& n): name(n) {}
    ~RosTopic() {}
  };
  
///////////////////////////////////////////////
// Edge class
///////////////////////////////////////////////

  class Edge {           
    friend ostream& operator<<(ostream& fout, Edge& e) {
      fout << "Edge{ " << e.vtx1 << ", " << e.vtx2 << ", " << e.stat << ", " << e.topic << " }";
      return fout;
    }

  public:
    int vtx1;    // id of end vertex 1
    int vtx2;    // id of end vertex 2
    eStat stat; // status of edge (from a viewpoint of graph search)
    RosTopic topic; // ros topic specific information
    
    Edge(): vtx1(-1), vtx2(-1), stat(EDGE_UN_VISITED) {}
    Edge(int v1, int v2, RosTopic &t): vtx1(v1), vtx2(v2), stat(EDGE_UN_VISITED), topic(t) {}

    bool operator!=(Edge& e) { return (vtx1 != e.vtx1 || vtx2 != e.vtx2); }
    bool operator==(Edge& e) { return (vtx1 == e.vtx1 && vtx2 == e.vtx2); }
    bool isIncidentOn(Vertex& v) { return (vtx1 == v.id || vtx2 == v.id); }
};

typedef list<Edge> EdgeList;    
typedef list<Edge>::iterator EdgeItor; 


///////////////////////////////////////////////
// TaskGraph class
///////////////////////////////////////////////

class TaskGraph {
 private:
  Vertex* pVtxArray;
  EdgeList* pAdjListArray;
 public:
  int max_vtx;
  int num_vtx;
  TaskGraph(): pVtxArray(NULL), pAdjListArray(NULL), max_vtx(0), num_vtx(0) {}
  TaskGraph(int m): pVtxArray(NULL), pAdjListArray(NULL) { init(m); }
  ~TaskGraph() {
  }
  void init(int m) {
    max_vtx = m;
    num_vtx = 0;
    pVtxArray = new Vertex[max_vtx];
    pAdjListArray = new EdgeList[max_vtx];
    for (int i=0; i < max_vtx; i++)
      pAdjListArray[i].clear();
  }
  void vertices(VtxList& list);  
  void edges(EdgeList& list);     
  bool insertVertex(Vertex& v);     
  bool insertEdge(Edge& e);  
  void incidentEdges(Vertex& v, EdgeList& elist);
  void print(); 
  //bool isAdjacentTo(Vertex v, Vertex w);
};

}

#endif //__TASK_GRAPH_H__
