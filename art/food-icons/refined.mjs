// Small-size silhouettes: broad planes, food-colored contours and selective texture.
// Every drawing is shared by the SVG preview and Android VectorDrawable.
import {p,line,ellipse,dot,group} from './drawing.mjs';
export const refined={};
const reg=(id,fn,spec={})=>refined[id]={family:fn.name,spec,draw:()=>fn(spec)};
const meat={base:'#E99894',shadow:'#CB7777',light:'#F8C1B3',fat:'#FFF0D9',edge:'#9B5559'};
const beef={base:'#B95050',shadow:'#873B43',light:'#E3877A',fat:'#FBE1C7',edge:'#743944'};
const chicken={base:'#F2C5AC',shadow:'#DCA68E',light:'#FFE6CC',fat:'#FFF0DB',edge:'#AD7966'};
const lamb={base:'#C87876',shadow:'#A35C61',light:'#EBAA94',fat:'#FFE6C9',edge:'#884D54'};

function cutStrips(s){
  const c=s.palette||meat, out=[];
  // Tapered, irregular cuts cross one another instead of resembling bacon ribbons.
  const pieces=[[43,24,-20,.91],[49,33,16,1.02],[44,42,-13,.95],[48,49,23,.88],[43,58,-12,.94],[48,66,8,.8],[43,72,-19,.66]];
  for(const[x,y,a,k]of pieces) out.push(group([
    p('M 14 34 Q 27 29 42 32 L 67 29 Q 75 30 78 34 L 72 39 L 45 40 Q 28 38 17 41 L 12 38 Z',c.shadow,c.edge,1.2),
    p('M 14 34 Q 27 29 42 32 L 67 29 Q 75 30 78 34 L 70 35 L 43 36 Q 25 34 15 38 Z',c.base,'none'),
    line('M 21 34 Q 30 33 38 34 M 49 34 L 66 32',c.light,1.7),
  ].map(node=>group([node],-45,-35)),x,y,k,k,a));
  return out;
}
reg('pork_strips',cutStrips);reg('beef_strips',cutStrips,{palette:beef});reg('chicken_strips',cutStrips,{palette:chicken});

function dicedMeat(s){
  const c=s.palette||meat,out=[];
  for(const[x,y,k,a]of [[27,20,.87,-9],[57,23,.8,14],[13,42,.88,6],[43,44,1.05,-9],[68,51,.74,12],[30,68,.83,5]]) {
    out.push(group([
      p('M -12 -5 Q -13 -9 -7 -12 L 7 -13 L 16 -5 L 14 10 L 1 16 L -13 8 Z',c.shadow,c.edge,1.3),
      p('M -12 -5 L -7 -12 L 7 -13 L 16 -5 L 1 2 Z',c.base,c.edge,1.1),
      p('M 1 2 L 16 -5 L 14 10 L 1 16 Z',c.base,'none'),
      p('M -7 -11 L -3 -12 L 6 -3 L 11 -5 L 14 -4 L 5 0 Z',c.fat,'none'),
      line('M -8 1 L -5 6 M 5 5 L 11 2',c.light,1.3),
    ],x,y,k,k,a));
  }
  return out;
}
reg('pork_cubes',dicedMeat);reg('beef_cubes',dicedMeat,{palette:beef});reg('lamb_cubes',dicedMeat,{palette:lamb});reg('chicken_cubes',dicedMeat,{palette:chicken});

function thinSlices(s){
  const c=s.palette||meat,out=[];
  for(const[x,y,a,k]of [[-7,-17,-5,.9],[0,0,4,1],[4,18,-9,.96]]) out.push(group([
    p('M 13 46 Q 25 29 49 31 Q 66 30 83 41 Q 82 50 67 58 Q 45 64 21 58 Z',c.shadow,c.edge,1.4),
    p('M 13 44 Q 25 28 49 30 Q 66 29 83 39 Q 81 47 65 53 Q 42 58 22 53 Z',c.base,c.edge,1.3),
    p('M 14 44 Q 22 33 37 31 L 36 35 Q 25 36 20 46 L 28 52 L 22 53 Z',c.fat,'none'),
    line('M 29 43 Q 42 33 53 38 M 39 48 Q 56 38 68 41',c.light,1.6),
  ],x,y,k,k,a));
  return out;
}
reg('pork_slices',thinSlices);reg('chicken_slices',thinSlices,{palette:chicken});
reg('fish_slices',thinSlices,{palette:{base:'#FFF3DF',shadow:'#D2D8D0',light:'#E2CCB2',fat:'#869E9D',edge:'#728985'}});

function leanCut(s){const c=s.palette||meat;return [
  p('M 13 51 Q 15 35 31 31 L 62 20 Q 80 20 82 38 L 79 54 L 43 77 Q 20 85 13 66 Z',c.shadow,c.edge,1.6),
  p('M 14 48 Q 17 36 33 32 L 63 22 Q 77 21 82 37 L 49 59 Q 28 59 14 48 Z',c.light,c.edge,1.4),
  p('M 16 51 Q 29 43 44 50 Q 58 60 43 74 Q 21 83 16 63 Z',c.fat,c.edge,1.4),
  p('M 21 53 Q 31 47 40 53 Q 50 59 39 69 Q 24 76 21 62 Z',c.base,'none'),
  line('M 28 55 Q 35 52 41 59 M 27 63 Q 31 66 37 62 M 42 34 L 68 25 M 48 42 L 73 31',c.fat,1.5),
];}
reg('lean_pork',s=>[group(leanCut(s),0,13,1,.8),...thinSlices(s).slice(-1).map(node=>group([node],21,34,.55))]);
reg('pork_loin',leanCut);reg('beef_tenderloin',leanCut,{palette:beef});

function bellyCuts(s){const out=[];for(const[x,y,k]of (s.bacon?[[-7,-16,.96],[-2,7,1],[6,30,.9]]:[[0,0,1]]))out.push(group([
  p('M 10 40 L 33 24 L 83 34 L 82 66 L 61 81 L 11 68 Z','#D27C75','#985954',1.5),
  p('M 10 40 L 33 24 L 83 34 L 61 50 Z','#FFEAD2','#985954',1.3),
  p('M 17 38 L 34 28 L 75 36 L 58 44 Z','#E4A193','none'),
  p('M 11 48 L 61 59 L 82 45 L 82 52 L 61 66 L 11 55 Z','#FFE9D0','none'),
  p('M 11 61 L 61 73 L 82 58 L 82 64 L 61 79 L 11 67 Z','#F9D7B9','none'),
  line('M 61 51 V 79','#B06C60',1.2),line('M 20 44 L 48 50','#F6BDB0',1.5),
],x,y,k,s.bacon?k*.53:k));return out;}
reg('pork_belly',bellyCuts);reg('bacon',bellyCuts,{bacon:true});

function chickenTenders(){const out=[];for(const[x,y,a]of [[7,-14,-18],[0,7,3],[-5,26,21]])out.push(group([
  p('M 15 46 Q 22 32 39 33 Q 57 32 79 47 Q 87 57 74 58 Q 45 50 25 57 Q 12 58 15 46 Z',chicken.base,chicken.edge,1.5),
  p('M 16 50 Q 32 45 50 47 L 77 55 Q 58 52 27 57 Q 17 58 16 50 Z',chicken.shadow,'none'),
  line('M 25 41 Q 42 35 59 44',chicken.light,2),
],x,y,1,1,a));return out;}reg('chicken_tender',chickenTenders);

function wingMid(){return [
  group([p('M 26 75 Q 17 64 23 43 L 36 22 Q 46 16 56 27 L 59 64 Q 54 81 39 84 Z','#EDC29A','#A77B58',1.5),
  p('M 41 28 Q 38 42 34 59 Q 33 71 38 78 L 46 76 Q 42 58 49 29 Z','#FBE2BF','none'),line('M 53 34 Q 59 61 48 74','#D3A47C',2)],-6,-2),
  group([p('M 28 77 Q 21 63 30 46 L 45 25 Q 59 22 66 35 L 62 66 Q 53 86 38 84 Z','#F6CFAB','#A77B58',1.5),
  line('M 45 33 Q 45 51 36 69 M 57 39 Q 58 55 49 73','#FFEDD1',2.6),dot(40,54,1.1,'#D5A77F'),dot(53,60,1.1,'#D5A77F')],13,6,.83),
];}reg('chicken_wing_mid',wingMid);
function wingRoot(){return [p('M 27 63 L 16 76 Q 9 76 10 82 Q 12 89 19 86 Q 25 91 29 85 L 41 70 Z','#FFF0D3','#AA7D5D',1.5),
  p('M 27 52 Q 26 34 46 23 Q 65 16 78 31 Q 91 49 75 62 Q 62 70 43 68 L 33 74 Z','#EEC5A3','#AA7D5D',1.6),
  p('M 74 30 Q 85 48 66 58 Q 50 66 33 66 L 33 74 Q 62 70 75 62 Q 90 47 80 35 Z','#D6A582','none'),
  line('M 36 48 Q 38 33 53 29 M 37 54 L 39 57','#FFE8CA',3),dot(61,38,1.1,'#C59A74'),dot(54,49,1.3,'#C59A74')];}reg('chicken_wing_root',wingRoot);
function feet(){const one=[p('M 37 15 L 48 15 L 48 47 L 69 63 Q 74 70 66 71 L 46 58 L 53 81 Q 54 88 48 85 L 37 63 L 30 82 Q 26 89 23 82 L 28 59 L 14 67 Q 6 68 10 61 L 32 46 Z','#E5B46F','#A77949',1.5),
  line('M 38 25 H 46 M 38 32 H 46 M 36 39 H 45 M 35 49 L 38 54 M 47 60 L 50 66 M 28 63 L 27 71','#FCE0A5',2)];return[group(one,5,0,.9,.9,-10),group(one,38,15,.68,.68,13)];}reg('chicken_feet',feet);
function neck(){return [p('M 25 18 Q 42 16 39 34 Q 33 49 45 50 Q 72 40 78 60 Q 84 77 65 83 L 54 71 Q 65 62 51 65 Q 20 69 21 45 L 25 32 Z','#CE8C79','#936053',1.7),
  line('M 25 31 L 37 34 M 22 42 L 34 44 M 25 54 L 37 51 M 33 62 L 41 53 M 48 62 L 45 51 M 57 60 L 56 49 M 64 62 L 70 54 M 64 70 L 77 69','#F0C3AA',2.5),ellipse(25,22,8,7,'#F0C3AA','#936053',1.3),ellipse(25,22,3,2.5,'#B78060','none')];}reg('duck_neck',neck);
function trotters(){return [p('M 24 17 Q 42 11 59 20 L 61 39 Q 79 47 81 64 Q 81 77 67 82 L 48 87 L 22 73 Q 14 63 21 48 L 28 37 Z','#E3AC96','#A16F61',1.6),
  p('M 32 23 Q 49 18 54 23 L 54 44 Q 72 54 71 65 L 61 68 L 38 57 L 31 43 Z','#F5CFB4','none'),
  p('M 22 59 L 47 69 L 47 85 L 24 73 Q 18 68 22 59 Z','#C49280','#A16F61',1.4),
  p('M 49 69 L 79 60 Q 86 76 65 82 L 49 87 Z','#D1A08C','#A16F61',1.4),line('M 35 26 L 49 28 M 33 36 L 47 38','#E6B6A0',1.6)];}reg('pork_trotters',trotters);

function balls(s){const c=s.beef?['#A97755','#84583F','#D4AA7F']:['#F8F1DD','#DCCDB1','#FFFCF1'],edge=s.beef?'#77563F':'#B5A589';return [
  ellipse(39,30,18,17,c[0],edge,1.4),p('M 49 17 Q 63 29 50 41 Q 40 48 28 39 Q 51 45 49 17 Z',c[1],'none'),
  ellipse(68,53,18,18,c[0],edge,1.4),p('M 79 42 Q 92 65 68 71 Q 59 72 53 64 Q 80 70 79 42 Z',c[1],'none'),
  ellipse(28,65,19,18,c[0],edge,1.4),line('M 18 61 Q 21 51 31 53',c[2],3),
  p('M 43 73 Q 44 54 61 56 Q 78 61 70 79 Z',c[1],edge,1.3),
  ellipse(57,74,16,9,c[2],edge,1.2),...[[49,72],[57,69],[64,75],[54,78]].map(([x,y])=>dot(x,y,1.1,c[1])),
];}reg('fish_balls',balls);reg('beef_balls',balls,{beef:true});

function riceCake(){return [
  p('M 12 43 L 57 20 Q 70 17 75 27 L 76 36 L 33 65 Q 20 72 13 61 Z','#F2EAD8','#B6A78E',1.4),
  p('M 12 43 L 57 20 Q 67 18 72 26 L 30 53 Q 20 56 12 49 Z','#FFFBEE','none'),
  ellipse(25,56,13,10,'#FFF8E9','#B6A78E',1.2),
  group([ellipse(65,62,19,11,'#DED2BA','#B6A78E',1.3),ellipse(65,58,19,11,'#FFFBEE','#B6A78E',1.3)],0,0,1,1,-13),
  ellipse(58,77,20,10,'#F8F0DE','#B6A78E',1.3),line('M 49 74 Q 58 71 66 75','#FFFFFF',2),
];}reg('rice_cake',riceCake);
function noodleNest(s){const out=[],thick=s.udon?4.5:s.rice?3:2.8,c=s.udon?'#FFF6DF':s.rice?'#FFFBEA':'#F2D39A',edge=s.udon?'#BAA78A':'#C5A46B';
  out.push(ellipse(47,63,34,15,'#EBD9B7','none'),p('M 15 58 Q 18 32 48 29 Q 72 29 81 53 L 80 67 Q 61 80 30 73 Z',c,'none'));
  const paths=['M 18 58 C 12 32 67 18 74 40 C 83 64 32 74 22 55 C 15 40 64 24 68 44 C 73 62 37 67 28 53',
    'M 20 66 C 35 81 81 65 77 45 C 74 26 29 38 26 49 C 19 68 62 70 66 50 C 70 34 38 36 34 49 C 28 61 55 63 58 51',
    'M 17 60 C 31 78 72 65 78 58 Q 88 51 84 72','M 30 67 Q 39 79 71 72 Q 83 69 81 80'];
  for(const d of paths)out.push(line(d,edge,thick+1.8),line(d,c,thick));
  out.push(line('M 27 40 Q 38 30 51 32 M 21 55 L 22 60',s.rice?'#FFFFFF':'#FFF1CF',1.4));return out;
}
reg('fresh_noodles',noodleNest);reg('rice_noodles',noodleNest,{rice:true});reg('udon',noodleNest,{udon:true});
function vermicelli(){const out=[];for(let i=0;i<10;i++){const x=18+i*4.5;out.push(line(`M ${x} 19 Q ${x-9} 35 ${x+1} 51 Q ${x+6} 64 ${x+19} 79`,'#C8BEA4',2.7),line(`M ${x} 19 Q ${x-9} 35 ${x+1} 51 Q ${x+6} 64 ${x+19} 79`,'#FFF8E2',1.4));}out.push(p('M 20 43 L 58 32 L 63 45 L 26 56 Z','#AAC2AA','#6E937D',1.2),line('M 25 46 L 56 37','#DCE7C9',2));return out;}reg('vermicelli',vermicelli);
function beanCurdSticks(){const out=[];for(const[x,y,a]of [[-11,-2,-16],[11,4,10],[24,15,25]])out.push(group([
  p('M 30 13 L 40 18 L 47 14 L 54 22 L 48 79 L 40 84 L 32 77 L 26 80 Z','#E6BB68','#AD813F',1.4),
  p('M 34 19 L 40 22 L 37 77 L 32 76 Z','#F8D994','none'),line('M 47 25 L 42 76 M 29 54 L 32 23','#C29649',1.4),
],x,y,.84,.84,a));return out;}reg('dried_tofu_skin',beanCurdSticks);

function spinach(){const out=[];
  const blades=[[-36,.85,'#61955D'],[34,.88,'#497D4C'],[-11,1,'#397849'],[19,.74,'#76A65F'],[-24,.65,'#86AC64']];
  for(const[a,k,c]of blades) out.push(group([
    line('M 0 0 Q 2 -13 0 -31','#8CB265',3.4),
    p('M 0 -24 L -12 -19 Q -8 -29 -14 -38 Q -20 -51 0 -73 Q 19 -54 15 -39 Q 8 -28 12 -20 Z',c,'#426C45',1.2),
    p('M 0 -24 Q -4 -42 0 -73 Q 19 -54 15 -39 Q 8 -28 12 -20 Z',c==='#397849'?'#4E8D56':'#77A366','none'),
    line('M 0 -24 Q -2 -43 0 -63 M 0 -42 L -10 -49 M 0 -35 L 10 -46','#BAD08C',1.4),
  ],48,80,k,k,a));
  out.push(p('M 42 79 L 45 89 L 48 84 L 51 90 L 54 79 Z','#CE9680','#9E7259',1.1));return out;
}reg('spinach',spinach);
function bokChoy(s){const green=s.shanghai?'#4E8D59':'#3D8253',out=[];for(const[x,y,a,k]of [[-14,-5,-23,.9],[14,1,24,.91],[0,9,0,1]])out.push(group([
  p('M 36 46 Q 29 64 31 77 Q 46 89 62 77 Q 62 62 56 46 Z',s.shanghai?'#C6DDA4':'#FAF3D9','#6D9870',1.2),
  p('M 31 47 C 15 33 26 12 43 18 Q 55 10 65 23 Q 78 39 57 52 Q 46 58 31 47 Z',green,'#346D48',1.3),
  line('M 46 76 Q 50 51 43 28 M 47 49 L 33 36 M 48 45 L 60 31',s.shanghai?'#ABD185':'#D2E0A7',1.8),
],x,y,k,k,a));return out;}reg('bok_choy',bokChoy);reg('shanghai_bok_choy',bokChoy,{shanghai:true});

function mushroomCaps(s){const c=s.white?'#F4ECD7':'#A7744E',edge=s.white?'#AF9A77':'#77553D';const one=[
  p('M 39 43 Q 42 62 32 77 Q 43 86 61 77 Q 53 62 55 43 Z','#F4E6C8',edge,1.4),
  p('M 50 48 Q 49 66 55 78 L 61 77 Q 53 59 55 44 Z','#D6C098','none'),
  line('M 43 57 Q 42 66 39 72','#CBB088',1.2),
  ellipse(47,46,32,9,'#D8C5A2',edge,1.2),
  p('M 14 43 Q 15 17 46 16 Q 74 16 81 43 Q 76 55 47 53 Q 23 54 14 43 Z',c,edge,1.5),
  p('M 16 44 Q 32 52 52 47 Q 72 44 77 34 L 81 43 Q 75 54 47 53 Q 22 54 16 44 Z',s.white?'#DDD0B4':'#875C3F','none'),
  p('M 24 33 Q 30 21 44 22 L 40 27 Q 30 27 28 35 Z',s.white?'#FFFBEC':'#CEA177','none'),
];if(!s.white)one.push(p('M 39 25 L 44 23 L 50 35 L 63 30 L 65 34 L 51 40 L 53 46 L 48 46 L 45 41 L 31 45 L 29 41 L 43 35 Z','#F7E1BB','none'));
return [group(one,-3,-3,.91,.91,-9),group(one,46,34,.55,.55,13)];}
reg('shiitake',mushroomCaps);reg('button_mushroom',mushroomCaps,{white:true});
