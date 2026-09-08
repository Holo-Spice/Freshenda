import {p,line,ellipse,dot,rect,group,scallop,star,hatch,leaf,seeds,ink,cream,shade,green,light,dark,red,pink,purple,brown,yellow,orange,blue} from './drawing.mjs';
export const produce={};
const reg=(id,fn,spec={})=>produce[id]={family:fn.name,spec,draw:()=>fn(spec)};

function leafy(s){
  const out=[], c=s.color||green, stem=s.stem||light;
  if(s.layout==='head'){
    out.push(p('M 21 53 C 17 35 29 21 47 20 C 69 17 81 35 76 57 Q 73 77 49 79 Q 27 78 21 53 Z',c));
    out.push(p('M 21 45 Q 28 53 37 50 Q 55 33 73 41 Q 81 57 61 73 Q 47 84 28 69 Z',s.inner||light));
    out.push(line('M 25 44 Q 42 38 51 23 M 29 48 Q 28 34 38 25 M 33 70 Q 50 56 69 47 M 43 61 Q 41 49 51 43 M 51 58 Q 60 63 69 54 M 48 77 Q 49 64 41 58',s.vein||cream,1.8));
    return out;
  }
  if(s.layout==='napa'){
    const h=s.h||60, w=s.w||48;out.push(group([p('M 20 79 Q 9 52 22 24 Q 26 15 34 22 Q 43 11 50 20 Q 61 13 67 24 Q 80 20 78 41 Q 79 66 68 79 Z',c),p('M 29 77 Q 17 43 32 29 Q 39 24 46 33 Q 53 20 60 30 Q 76 35 61 78 Z',s.inner||light),p('M 29 77 Q 37 49 34 31 Q 43 48 45 66 Q 52 45 60 30 Q 59 53 61 78 Z',cream),line('M 35 76 L 35 53 M 47 77 L 49 50 M 57 77 L 58 59',shade,1.1)],48-w/2-20*w/58,79-h,w/58,h/64));return out;
  }
  if(s.layout==='rosette'){
    for(let j=0;j<9;j++)out.push(leaf(48,52,s.w||19,s.h||36,-150+j*37,c,s.form||'oval'));
    for(let j=0;j<5;j++)out.push(leaf(48,58,15,27,-90+j*42,s.inner||light,s.form||'oval'));
    return out;
  }
  if(s.layout==='vine'){
    for(let b=0;b<(s.count||2);b++){
      const x=31+b*20;out.push(line(`M ${x+5} 80 Q ${x-3} 53 ${x+9} 20`,ink,3.8),line(`M ${x+5} 80 Q ${x-3} 53 ${x+9} 20`,stem,2));
      for(let j=0;j<3;j++)out.push(leaf(x+3,67-j*17,s.w||23,s.h||24,j%2?-53:55,c,s.form||'oval',s.vein||dark));
    }
    if(s.tendril)out.push(line('M 59 40 C 82 25 84 49 72 46 C 65 43 72 37 77 42',dark,1.4));
  }else{
    const count=s.count||5;
    for(let j=0;j<count;j++){
      const a=-40+j*80/(count-1),x=48+Math.sin(a*Math.PI/180)*15,y=s.low?67:58;
      out.push(line(`M ${44+j*2} 79 Q ${x} 67 ${x} ${y-5}`,ink,s.thick?8:3.3),line(`M ${44+j*2} 79 Q ${x} 67 ${x} ${y-5}`,stem,s.thick?5.5:1.5));
      out.push(leaf(x,y,s.w||22,(s.h||39)-(j%2)*4,a,c,s.form||'oval',s.vein||dark));
      if(s.center)out.push(group([p('M 0 0 Q -11 -11 0 -29 Q 11 -11 0 0 Z',s.center,'none')],x,y,1,1,a));
    }
    if(s.root)out.push(p('M 43 76 L 47 86 L 51 78 L 54 85 L 55 76 Z',red,ink,1.2));
  }
  if(s.flowers){out.push(line('M 48 66 L 59 23',dark,2));for(const [x,y]of [[58,23],[64,28],[55,30]])out.push(star(x,y,4,2,5,yellow,1));}
  if(s.hollow)out.push(ellipse(45,80,3,1.8,cream,ink,1));
  return out;
}
const leafSpecs={
spinach:{form:'arrow',w:24,h:44,root:true,color:'#688854'},bok_choy:{w:26,h:33,stem:cream,thick:true,count:4},shanghai_bok_choy:{w:28,h:32,stem:'#BED299',thick:true,count:5,low:true},choy_sum:{layout:'vine',w:21,h:29,flowers:true},baby_bok_choy:{w:28,h:29,form:'curly',stem:cream,thick:true,low:true,count:4},tatsoi:{layout:'rosette',color:dark,w:17,h:32},quick_choy:{w:21,h:39,stem:cream,thick:true,count:4},baby_greens:{w:13,h:27,count:8,stem:light},napa_cabbage:{layout:'napa',color:'#A8BC80',h:64,w:53},baby_napa:{layout:'napa',color:'#CACD8B',inner:'#E2DCA0',h:53,w:43},romaine:{h:53,w:23,stem:cream,count:4},iceberg_lettuce:{layout:'head',color:'#A9C58B'},green_leaf_lettuce:{form:'curly',w:33,h:41,count:4},red_leaf_lettuce:{form:'curly',w:33,h:41,color:'#927568',stem:light,count:4},butter_lettuce:{layout:'rosette',w:30,h:33,color:'#9FBC82',inner:'#CED89D'},youmai:{w:12,h:57,count:7},mustard_greens:{form:'serrated',w:34,h:47,count:3,vein:cream},chinese_kale:{layout:'vine',w:30,h:28,stem:'#99B77D',color:'#61806B'},gai_lan_tips:{layout:'vine',w:17,h:22,count:3},chrysanthemum_greens:{form:'lobed',w:28,h:50,count:4},water_spinach:{layout:'vine',form:'arrow',w:17,h:31,hollow:true},red_amaranth:{layout:'vine',w:26,h:29,color:'#866A78',stem:red},green_amaranth:{layout:'vine',w:24,h:29,count:3},sweet_potato_leaves:{layout:'vine',form:'heart',w:24,h:26,tendril:true},malabar_spinach:{layout:'vine',form:'heart',w:29,h:25,color:'#557B58',count:2},celtuce_leaves:{form:'curly',w:16,h:55,count:5},turnip_greens:{form:'lobed',w:30,h:45,count:4,root:true},beet_greens:{w:32,h:42,stem:red,vein:'#BA7563',count:3},kale:{form:'curly',w:33,h:50,color:'#59795A',count:3},swiss_chard:{w:34,h:46,stem:'#D99858',vein:'#DCB573',thick:true,count:3},collards:{w:40,h:45,color:'#789676',count:3,vein:cream},shepherds_purse:{layout:'rosette',form:'lobed',w:14,h:36},purslane:{layout:'vine',w:13,h:13,count:3,stem:red},pea_shoots:{layout:'vine',w:18,h:21,tendril:true},goji_leaves:{layout:'vine',w:12,h:24,count:3},toon_shoots:{layout:'vine',form:'serrated',w:15,h:28,color:'#9E6870',stem:red,count:3},watercress:{layout:'vine',w:19,h:17,count:3},endive:{layout:'rosette',form:'lobed',w:14,h:37},chicory:{form:'serrated',w:19,h:49,count:5},radicchio:{layout:'head',color:'#815B76',inner:'#AB7C90',vein:cream}
};for(const[id,s]of Object.entries(leafSpecs))reg(id,leafy,s);

function broccoli(s){
 const out=[];
 if(s.romanesco){out.push(p('M 26 77 Q 30 58 48 65 Q 64 57 73 77 Z',light));for(const[x,y,r]of [[30,62,13],[67,61,13],[45,60,20],[51,38,12]]){out.push(p(`M ${x-r} ${y+10} Q ${x-r+4} ${y-2} ${x} ${y-r-10} Q ${x+r-4} ${y} ${x+r} ${y+10} Z`,light));for(let j=0;j<4;j++)out.push(line(`M ${x-r+j*2} ${y+8-j*5} q ${r-j*2} -5 ${r*2-j*4} 0`,dark,.8));}return out;}
 out.push(p('M 36 82 Q 40 62 31 53 L 40 48 L 48 60 L 53 47 L 63 51 Q 57 65 61 82 Q 48 87 36 82 Z',light),line('M 44 79 L 46 65 L 39 56 M 53 79 L 51 65 L 58 54',dark,1.2));
 if(s.white)out.push(leaf(33,77,28,39,-35,green),leaf(62,77,29,40,36,green));
 for(const[x,y,rx,ry]of (s.loose?[[24,44,12,10],[38,26,12,11],[59,27,13,10],[73,47,12,10],[48,43,14,12]]:[[25,43,14,14],[38,28,16,14],[60,30,17,15],[74,46,13,13],[49,45,22,17]])){
   if(s.loose)out.push(line(`M 48 66 L ${x} ${y}`,light,6));out.push(scallop(x,y,rx,ry,11,.15,s.white?cream:green));
   for(let j=0;j<7;j++){const a=j*2.4,r=(j%3+1)*3;out.push(line(`M ${+(x+Math.cos(a)*r-2).toFixed(2)} ${+(y+Math.sin(a)*r).toFixed(2)} q 2 -3 4 0`,s.white?shade:dark,.85));}
 }return out;
}
reg('broccoli',broccoli);reg('cauliflower',broccoli,{white:true});reg('loose_cauliflower',broccoli,{white:true,loose:true});reg('romanesco',broccoli,{romanesco:true});

function rootVegetable(s){const out=[],c=s.color||orange;
 if(s.type==='carrot'){
 out.push(group([leaf(57,33,15,24,-20,green,'lobed'),leaf(57,33,13,27,20,light,'lobed'),leaf(57,33,14,24,53,green,'lobed'),p('M 53 29 Q 68 28 70 39 Q 67 52 19 80 Q 14 82 17 77 Q 35 44 53 29 Z',orange),p('M 57 33 Q 41 53 24 72 Q 44 60 62 36 Z','#E9B271','none'),line('M 44 44 l 8 3 M 37 54 l 6 3 M 28 65 l 5 2 M 56 41 l 5 3 M 48 55 l 5 2',brown,1.3)],0,0));return out;}
 if(s.type==='burdock'){
 for(const[x,y,k]of [[-4,0,.9],[20,7,.85]])out.push(group([p('M 16 72 L 69 14 Q 75 13 78 18 L 25 81 Q 18 83 16 72 Z',brown),ellipse(21,77,5,4,cream),line('M 32 65 l -5 -2 M 44 49 l -5 -2 M 55 35 l -5 -2 M 37 65 l 3 3 M 49 50 l 4 2',shade,.9)],x,y,k));
 out.push(ellipse(67,74,14,7,cream),line('M 56 74 L 78 73',shade,1));return out;}
 if(s.type==='yam'){
 out.push(p('M 17 67 L 60 18 Q 65 14 71 22 L 32 78 Q 21 83 17 67 Z',brown));out.push(ellipse(25,72,10,7,cream));for(let i=0;i<8;i++)out.push(line(`M ${31+i*4} ${61-i*5} l -6 -2 m 3 5 l -5 3`,shade,.8));out.push(group([ellipse(66,69,16,9,cream),ellipse(66,66,16,9,cream)],0,0));return out;}
 if(s.type==='lotus'){
 out.push(p('M 17 57 Q 14 42 29 37 L 46 26 Q 63 21 68 34 L 70 47 Q 63 62 48 62 L 33 73 Q 22 76 17 57 Z',shade),line('M 32 39 Q 39 46 39 61 M 47 29 Q 55 39 54 53',brown,2));out.push(ellipse(66,67,20,17,cream));for(let i=0;i<7;i++){const a=i*Math.PI*2/7;out.push(ellipse(+(66+Math.cos(a)*12).toFixed(2),+(67+Math.sin(a)*10).toFixed(2),3,4,shade,ink,1));}out.push(dot(66,67,2,shade));return out;}
 if(s.type==='potato'){
 for(const[x,y,scale,angle]of [[-3,-10,.85,-12],[11,9,1,8]])out.push(group([p('M 22 37 C 40 23 65 29 72 42 C 85 54 69 72 50 73 C 35 80 17 68 17 54 Q 14 44 22 37 Z',c),p('M 22 46 Q 25 35 41 34 L 48 37 Q 28 39 24 51 Z',s.highlight||'#EAD3A0','none'),line('M 32 45 q 3 -3 5 0 M 58 39 q 3 -2 5 1 M 49 62 q 3 -3 5 0 M 28 60 l 2 -2 M 64 55 l 2 -2',brown,1.3)],x,y,scale,scale,angle));return out;}
 if(s.type==='sweet'){
 out.push(p('M 15 66 Q 21 48 46 29 Q 72 12 73 35 Q 66 58 34 73 Q 18 81 15 66 Z',c),line('M 26 58 q 11 -6 16 -14 M 48 55 l 8 -7 M 59 31 l 6 -5',brown,1));out.push(ellipse(64,67,20,16,c));out.push(ellipse(64,65,17,14,s.inside));out.push(line('M 51 67 Q 62 52 74 64',s.inside===purple?'#BA91AF':'#F4C681',1.4));return out;}
 if(s.type==='taro'||s.type==='chestnut'){
 out.push(p('M 19 53 Q 22 30 42 27 Q 64 30 64 51 Q 65 71 43 76 Q 22 76 19 53 Z',brown));for(let i=0;i<5;i++)out.push(line(`M ${23+i%2*2} ${39+i*7} Q 42 ${45+i*7} ${60-i%2*2} ${37+i*7}`,shade,1.3));out.push(ellipse(67,65,17,14,cream));if(s.type==='taro')for(let i=0;i<12;i++)out.push(line(`M ${55+i%4*7} ${58+Math.floor(i/4)*6} l 2 -1`,purple,.8));return out;}
 if(s.type==='bamboo'){
 out.push(p('M 18 77 Q 26 43 50 15 Q 68 35 72 76 Z',brown));for(let j=0;j<4;j++)out.push(p(`M ${21+j*6} ${73-j*12} Q 46 ${60-j*11} ${68-j*5} ${72-j*12} Q 49 ${84-j*13} ${21+j*6} ${73-j*12} Z`,j%2?shade:yellow,ink,1.4));out.push(p('M 58 79 L 69 32 Q 82 50 85 75 Z',cream));out.push(line('M 64 72 L 79 67 M 66 63 L 76 59 M 68 53 l 6 -4',shade,1.2));return out;}
 if(s.type==='celtuce'||s.type==='water_bamboo'){
 out.push(leaf(54,43,23,40,35,green,'serrated'),leaf(44,38,23,33,-27,light));out.push(p('M 38 34 Q 48 29 59 36 L 62 79 Q 48 84 33 77 Z',s.type==='celtuce'?light:cream));out.push(line('M 39 45 l 8 4 M 55 54 l -8 4 M 37 65 l 10 4',s.type==='celtuce'?dark:shade,1));return out;}
 if(s.type==='kohlrabi'){out.push(leaf(27,46,19,28,-45,green),leaf(48,32,16,24,0,green),leaf(65,42,17,26,44,green));out.push(ellipse(48,60,28,24,light),line('M 25 54 q 4 -3 7 0 M 62 47 q 3 0 5 4 M 41 81 l -3 4',dark,1.3));return out;}
 out.push(leaf(41,33,17,29,-25,green,'lobed'),leaf(49,32,17,31,18,light,'lobed'),leaf(55,34,15,27,40,green));
 const round=s.type==='round';out.push(p(round?'M 27 38 Q 46 22 65 37 Q 78 54 62 69 Q 49 81 30 68 Q 17 56 27 38 Z':'M 32 33 Q 48 27 64 36 Q 67 60 47 78 L 37 84 Q 40 78 35 68 Q 24 52 32 33 Z',c));out.push(line(round?'M 45 74 q 9 4 5 11':'M 42 81 l -3 4',brown,1.3),line('M 33 46 q 7 -3 12 0 M 47 61 q 6 -2 11 -1',s.vein||shade,1.2));
 if(s.inside){out.push(ellipse(67,65,18,17,cream),ellipse(67,65,14,13,s.inside,ink,1));out.push(...seeds(67,65,8,7,7,cream));}return out;
}
for(const[id,s]of Object.entries({carrot:{type:'carrot'},white_radish:{color:cream},green_radish:{color:light},watermelon_radish:{type:'round',color:light,inside:'#C2677E'},cherry_radish:{type:'round',color:red},beetroot:{type:'round',color:'#9D5B61',vein:pink},potato:{type:'potato',color:'#D5B67E'},sweet_potato:{type:'sweet',color:'#AD7565',inside:orange},purple_sweet_potato:{type:'sweet',color:'#77546E',inside:purple},yam:{type:'yam'},taro:{type:'taro'},lotus_root:{type:'lotus'},celtuce:{type:'celtuce'},kohlrabi:{type:'kohlrabi'},bamboo_shoot:{type:'bamboo'},water_bamboo:{type:'water_bamboo'},water_chestnut:{type:'chestnut'},burdock:{type:'burdock'}}))reg(id,rootVegetable,s);
reg('green_cabbage',leafy,{layout:'head',color:green,inner:light});reg('red_cabbage',leafy,{layout:'head',color:'#795973',inner:'#AC7D9C',vein:cream});
function sproutsCabbage(){return [[-1,12,.62],[35,20,.57],[15,-10,.62]].map(([x,y,k])=>group(leafy({layout:'head',color:green,inner:light}),x,y,k));}reg('brussels_sprouts',sproutsCabbage);
function stalks(s){const out=[];for(let i=0;i<(s.count||4);i++){const x=25+i*12,y=23+i%2*4;out.push(p(`M ${x} 79 L ${x+3} ${y+9} L ${x+7} ${y-5} L ${x+12} ${y+10} L ${x+8} 79 Z`,light));out.push(line(`M ${x+4} 74 L ${x+6} ${y+10}`,dark,1));if(s.type==='asparagus')for(let j=0;j<3;j++)out.push(p(`M ${x+3} ${y+4+j*5} l 4 -4 l 3 5`,green,ink,1));else out.push(leaf(x+6,y+17,22,28,(i-1)*20,green,'lobed'));}out.push(line('M 25 69 Q 46 73 68 70',shade,3));return out;}
reg('asparagus',stalks,{type:'asparagus'});reg('celery',stalks,{type:'celery',count:4});reg('chinese_celery',leafy,{layout:'vine',form:'lobed',w:20,h:23,count:3});

function tomato(s){const out=[];for(const[x,y,k]of (s.small?[[2,3,.68],[27,-7,.63],[29,24,.62]]:[[-6,-8,.83],[20,14,.82]]))out.push(group([p('M 19 46 Q 22 27 42 31 Q 51 24 61 33 Q 79 37 78 57 Q 76 79 49 80 Q 21 82 18 61 Q 14 54 19 46 Z',red),p('M 60 38 Q 77 54 65 69 Q 53 81 34 72 Q 58 77 66 58 Z','#B45D4D','none'),line('M 27 47 Q 31 37 38 39 M 26 54 l 0 4',cream,2),star(47,34,14,4.5,6,green),line('M 47 34 Q 49 24 54 20',dark,2.4)],x,y,k,s.small?k*1.15:k));return out;}reg('tomato',tomato);reg('cherry_tomato',tomato,{small:true});
function elongated(s){const out=[],c=s.color||green;
 if(s.type==='eggplant'){
 out.push(p(s.round?'M 27 36 C 8 59 21 82 48 83 C 76 85 85 55 65 35 Z':'M 60 20 Q 75 25 69 43 Q 59 65 31 78 Q 13 84 14 67 Q 18 56 36 49 Q 52 39 54 24 Z',c));out.push(p(s.round?'M 26 51 Q 22 68 34 74 L 39 75 Q 27 58 33 48 Z':'M 22 66 Q 45 56 57 37 Q 50 55 29 73 Z',s.highlight||'#B496B2','none'));out.push(star(s.round?47:62,s.round?34:24,s.round?19:13,6,5,green));out.push(line(s.round?'M 48 31 Q 45 19 54 15':'M 62 25 Q 59 16 65 11',dark,3));return out;}
 if(s.type==='pepper'){
 if(s.bell){out.push(p('M 27 33 Q 32 24 43 29 Q 52 22 62 29 Q 77 28 77 42 L 71 71 Q 69 82 58 79 Q 50 86 41 78 Q 26 80 23 67 L 20 43 Q 19 35 27 33 Z',c),line('M 42 32 Q 35 50 41 74 M 57 32 Q 64 53 59 77',ink,1.4),p('M 29 38 Q 24 47 29 60 L 33 60 Q 30 44 35 39 Z',s.highlight||light,'none'));}
 else {out.push(p(s.twist?'M 24 28 Q 40 24 43 37 Q 37 47 55 45 Q 74 46 69 62 Q 65 75 81 74 Q 63 88 57 71 Q 60 59 42 57 Q 27 59 29 44 Q 16 44 24 28 Z':'M 24 28 Q 37 25 41 39 Q 53 68 79 71 Q 56 84 40 64 Q 16 47 24 28 Z',c),line('M 29 36 Q 32 57 56 69',s.highlight||light,1.6));}out.push(line('M 27 29 Q 19 24 24 16',dark,4));return out;}
 if(s.type==='bottle'||s.type==='butternut'){out.push(p('M 44 20 Q 55 16 60 24 L 58 39 Q 58 47 70 51 Q 85 67 70 80 Q 48 92 34 76 Q 24 62 37 48 Q 45 42 40 31 Q 37 24 44 20 Z',c),line('M 46 22 L 47 16',brown,4),line('M 43 54 Q 34 65 43 75',s.highlight||light,2));return out;}
 if(s.type==='chayote'){out.push(p('M 22 64 Q 14 52 30 40 L 45 21 Q 51 18 56 25 Q 66 18 70 30 Q 79 37 74 48 Q 85 64 68 75 Q 41 87 22 64 Z',light),line('M 50 26 Q 45 44 51 53 Q 39 61 43 75 M 64 33 Q 64 49 54 60',dark,1.6));return out;}
 out.push(p('M 15 65 Q 17 53 34 43 L 63 19 Q 74 13 82 27 Q 86 36 74 45 L 35 76 Q 18 87 15 65 Z',c));
 if(s.ridges)for(let i=0;i<3;i++)out.push(line(`M ${20+i*5} ${67+i*2} Q ${43+i*5} 48 ${69+i*3} ${23+i*4}`,s.highlight||light,1.3));
 if(s.bumpy)for(let j=0;j<22;j++){const x=26+(j%6)*7,y=65-(j%6)*6+Math.floor(j/6)*3;out.push(ellipse(x,y,2,1.3,light,dark,.6));}
 if(s.slice){out.push(ellipse(65,70,20,14,c));out.push(ellipse(65,68,17,12,s.inside||'#D4DDA9'));if(s.star){out.push(star(65,68,11,5,5,cream));out.push(...seeds(65,68,5,4,5,shade));}else out.push(...seeds(65,68,8,5,6,cream));}return out;}
for(const[id,s]of Object.entries({cucumber:{ridges:true,bumpy:true,slice:true,color:'#628453'},mini_cucumber:{slice:true,color:green},long_eggplant:{type:'eggplant',color:'#76567B'},round_eggplant:{type:'eggplant',round:true,color:'#735674'},green_eggplant:{type:'eggplant',round:true,color:light,highlight:'#D0DCAA'},green_pepper:{type:'pepper',bell:true},red_bell_pepper:{type:'pepper',bell:true,color:red,highlight:'#E4A28A'},yellow_bell_pepper:{type:'pepper',bell:true,color:yellow,highlight:'#F2D493'},hot_green_pepper:{type:'pepper'},hang_pepper:{type:'pepper',color:'#9DB475'},screw_pepper:{type:'pepper',twist:true},okra:{ridges:true,slice:true,star:true},zucchini:{ridges:true,slice:true,color:light},butternut:{type:'butternut',color:'#CDA571'},winter_melon:{ridges:true,slice:true,color:'#628D76',inside:cream},loofah:{ridges:true,color:'#98AE73'},bitter_melon:{bumpy:true,color:light},bottle_gourd:{type:'bottle',color:light},chayote:{type:'chayote'}}))reg(id,elongated,s);
function chilies(){return [[0,7,.67],[20,10,.74],[37,0,.7]].map(([x,y,k])=>group(elongated({type:'pepper',color:red,highlight:pink}),x,y,k));}reg('xiaomila',chilies);
function pumpkin(s){const out=[];for(const[x,rx]of [[26,15],[69,15],[38,19],[59,18],[48,17]])out.push(ellipse(x,55,rx,27,s.color||orange));out.push(p('M 42 30 Q 47 24 44 16 L 53 13 Q 50 22 56 29 Z',brown));out.push(line('M 24 47 Q 21 57 27 67 M 42 36 Q 37 50 39 65 M 69 44 Q 76 59 68 67',s.highlight||yellow,1.5));return out;}reg('winter_squash',pumpkin);reg('kabocha',pumpkin,{color:'#597A5C',highlight:light});

function beans(s){const out=[];if(s.type==='sprout'){for(let i=0;i<7;i++){const x=20+i*8,y=26+i%3*6;out.push(line(`M ${x+3} 78 Q ${x+11} 56 ${x} ${y+3}`,ink,s.thick?5:3.5),line(`M ${x+3} 78 Q ${x+11} 56 ${x} ${y+3}`,cream,s.thick?3.5:2));if(s.leaves){out.push(leaf(x,y+8,10,s.big?18:11,-45,green),leaf(x,y+8,10,s.big?18:11,45,light));}else out.push(ellipse(x,y,s.big?5:3,s.big?4:2.5,yellow,ink,1));}return out;}
 if(s.type==='corn'){out.push(p('M 35 76 Q 25 40 43 20 Q 49 14 57 21 Q 72 38 60 75 Z',yellow));for(let j=0;j<7;j++)for(let i=0;i<3;i++)out.push(rect(36+i*8,27+j*6,7,5,2,j%2?yellow:'#EDCF89',brown,.65));out.push(p('M 17 39 Q 27 43 49 79 Q 18 82 17 39 Z',green),p('M 78 31 Q 64 48 49 80 Q 80 68 78 31 Z',light),line('M 22 49 Q 34 69 44 75 M 72 44 Q 68 65 55 75',dark,1.2));return out;}
 if(s.type==='long'){for(let j=0;j<3;j++){const d=`M ${22+j*5} ${22+j*6} C ${78-j*3} ${18+j*5} ${82-j*3} ${76-j*6} ${38+j*5} ${79-j*6} C ${5+j*4} ${76-j*6} 30 ${44+j*3} 57 ${45+j*4}`;out.push(line(d,ink,6),line(d,green,3.6));}return out;}
 const c=s.color||green;
 for(let j=0;j<(s.open?1:2);j++){const yy=j*22;out.push(group([p(s.flat?'M 15 55 Q 40 12 80 31 Q 61 68 15 55 Z':'M 14 54 Q 35 26 56 28 Q 70 25 81 33 Q 64 57 42 65 Q 22 68 14 54 Z',c),line('M 19 53 Q 47 36 76 34',light,1.5)],0,yy));}
 if(s.open||s.flat){for(let i=0;i<5;i++)out.push(ellipse(29+i*9,49-i*3,s.big?6:4,s.big?5:4,light,ink,1));}
 if(s.hairy)for(let i=0;i<8;i++)out.push(line(`M ${22+i*7} ${54-(i%4)*4} l -3 -4`,shade,.8));
 if(s.big){out.push(ellipse(61,76,10,7,light),ellipse(77,69,9,7,light),line('M 56 76 q 4 -3 7 0 M 74 68 q 3 -2 6 1',dark,1));}return out;}
for(const[id,s]of Object.entries({green_bean:{},wax_bean:{color:yellow},yardlong_bean:{type:'long'},snow_pea:{flat:true},sugar_snap_pea:{color:'#99B772'},green_pea:{open:true},edamame:{hairy:true,open:true},fava_bean:{open:true,big:true},lima_bean:{flat:true,big:true},hyacinth_bean:{flat:true,color:'#A58BA0'},corn:{type:'corn'},mung_sprout:{type:'sprout'},soy_sprout:{type:'sprout',thick:true,big:true},alfalfa_sprout:{type:'sprout',leaves:true},sunflower_shoot:{type:'sprout',leaves:true,thick:true,big:true}}))reg(id,beans,s);

function allium(s){const out=[];
 if(s.type==='leek'){
 out.push(p('M 36 53 Q 22 38 18 14 L 25 15 L 43 44 L 38 10 L 45 10 L 52 44 L 62 13 L 70 15 L 59 49 L 78 26 L 83 30 L 63 59 Z',green));
 out.push(p('M 35 49 Q 49 58 63 49 L 63 82 Q 49 88 35 82 Z',cream),line('M 43 57 V 81 M 55 57 V 81',shade,1.2),line('M 22 19 L 42 53 M 43 18 L 48 52 M 66 21 L 56 52',light,1));return out;}
 if(s.type==='onion'){out.push(p('M 18 51 Q 21 32 40 29 L 43 17 L 51 22 Q 50 31 60 33 Q 74 44 69 64 Q 64 78 41 78 Q 19 77 18 51 Z',s.color||shade),line('M 38 32 Q 23 57 38 75 M 48 32 Q 61 56 49 75 M 42 78 l -5 7 M 46 78 l 1 7 M 49 77 l 6 6',s.vein||brown,1.1));out.push(ellipse(66,65,19,18,cream));for(let i=0;i<4;i++)out.push(ellipse(66,65,15-i*3,14-i*3,'none',s.color||brown,1.2));return out;}
 if(s.type==='garlic'){out.push(p('M 19 58 Q 21 42 35 40 L 43 19 L 48 20 L 48 39 Q 68 40 72 58 Q 75 79 47 82 Q 17 81 19 58 Z',cream),p('M 45 42 Q 32 60 40 79 L 48 80 Q 60 56 45 42 Z',shade),line('M 32 45 Q 23 63 34 74 M 56 45 Q 70 61 57 75 M 39 82 l -2 4 M 44 82 l 1 4',brown,1.1));out.push(p('M 66 81 Q 60 68 74 58 Q 87 68 83 78 Q 78 85 66 81 Z',cream));return out;}
 if(s.type==='ginger'){out.push(p('M 17 66 Q 11 51 30 50 L 31 39 Q 21 24 36 22 Q 47 22 46 40 L 56 39 Q 61 17 73 24 Q 82 32 66 47 Q 84 48 82 61 Q 73 69 62 62 L 52 70 Q 58 81 43 83 Q 29 83 31 71 Z',shade));out.push(line('M 24 54 l 5 10 M 34 33 l 9 -1 M 38 48 l 8 5 M 53 56 l 10 4 M 65 29 l 5 5 M 37 73 l 10 1',brown,1.4));return out;}
 const c=s.color||green;
 if(s.type==='scapes'){for(let j=0;j<4;j++){out.push(line(`M ${27+j*9} 82 Q ${20+j*10} 46 ${32+j*10} 22 Q ${42+j*9} 12 ${43+j*9} 27`,ink,4),line(`M ${27+j*9} 82 Q ${20+j*10} 46 ${32+j*10} 22 Q ${42+j*9} 12 ${43+j*9} 27`,green,2.2));}return out;}
 for(let j=0;j<(s.count||5);j++){const x=26+j*9;out.push(p(`M ${x} 79 Q ${x-4} 40 ${x+7} 16 L ${x+9} 18 Q ${x+4} 50 ${x+6} 78 Z`,c));if(s.white)out.push(p(`M ${x} 56 L ${x+6} 56 L ${x+6} 79 Q ${x+3} 82 ${x} 79 Z`,cream,ink,1.2));if(s.flower)out.push(p(`M ${x+6} 20 Q ${x-3} 10 ${x+9} 11 Q ${x+14} 15 ${x+6} 20 Z`,light));if(s.roots)out.push(line(`M ${x+2} 80 l -3 5 M ${x+4} 80 l 1 6`,brown,.8));}return out;}
for(const[id,s]of Object.entries({yellow_onion:{type:'onion',color:'#CFAC74'},red_onion:{type:'onion',color:purple,vein:cream},white_onion:{type:'onion',color:cream},scallion:{white:true,roots:true,count:6},welsh_onion:{white:true,roots:true,count:3},leek:{type:'leek'},chive:{count:7},yellow_chive:{count:7,color:'#DAD19C'},garlic_chive_flower:{count:5,flower:true},garlic:{type:'garlic'},garlic_shoot:{type:'scapes'},green_garlic:{white:true,count:4},ginger:{type:'ginger'}}))reg(id,allium,s);
for(const[id,s]of Object.entries({cilantro:{layout:'vine',form:'lobed',w:23,h:22,count:3},parsley:{layout:'vine',form:'lobed',w:23,h:30,count:2},basil:{layout:'vine',form:'oval',w:25,h:29,count:2},mint:{layout:'vine',form:'serrated',w:25,h:28,count:2},perilla:{form:'serrated',w:37,h:44,count:3,color:purple,stem:'#9D7C8F'}}))reg(id,leafy,s);
function wispyHerb(s){const out=[];for(let b=0;b<3;b++){const x=31+b*15;out.push(line(`M ${x-5} 81 Q ${x+2} 50 ${x} 17`,s.wood?brown:dark,2));for(let j=0;j<7;j++){const y=25+j*7;for(const dir of [-1,1]){out.push(line(`M ${x} ${y+7} q ${dir*8} -3 ${dir*12} -11`,green,s.wood?3:1.1));if(!s.wood)out.push(line(`M ${x+dir*6} ${y+2} l ${dir*3} -8 m -1 5 l ${dir*7} -1`,green,.8));}}}return out;}
reg('rosemary',wispyHerb,{wood:true});reg('dill',wispyHerb,{});
