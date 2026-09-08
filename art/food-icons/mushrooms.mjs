import {p,line,ellipse,dot,group,scallop,droplet,ink,cream,shade,green,dark,brown} from './drawing.mjs';
export const fungi={};const reg=(id,fn,spec={})=>fungi[id]={family:fn.name,spec,draw:()=>fn(spec)};
function capMushroom(s){const out=[],c=s.color||brown;
 const mushroom=(x,y,k)=>group([p('M 39 42 L 56 42 Q 53 66 61 79 Q 50 88 33 80 Q 40 67 39 42 Z',cream),p('M 50 47 L 55 47 Q 52 70 57 79 L 49 81 Z',shade,'none'),ellipse(47,45,31,10,shade),...Array.from({length:11},(_,i)=>line(`M ${20+i*5} 43 L ${44+(i%3)*3} 50`,brown,.7)),p(s.round?'M 16 43 Q 15 14 47 13 Q 78 14 79 43 Q 54 56 16 43 Z':'M 13 44 Q 19 21 36 20 Q 48 13 63 23 Q 77 29 81 44 Q 64 51 48 45 Q 26 55 13 44 Z',c),p('M 20 38 Q 28 24 40 23 Q 29 28 25 41 Z',s.highlight||shade,'none'),line('M 41 56 Q 45 65 40 76 M 47 60 L 45 73',brown,.7)],x,y,k);
 out.push(mushroom(-6,-3,.9));out.push(mushroom(45,38,.48));
 if(s.crack)out.push(group([p('M 45 21 L 40 31 L 27 35 L 40 36 L 42 45 L 49 36 L 62 38 L 54 31 L 58 23 L 48 29 Z',cream,brown,.7)],-6,-3,.9));
 if(s.water)out.push(...droplet());return out;
}
reg('button_mushroom',capMushroom,{color:cream,round:true,highlight:'#FFF9E9'});reg('cremini',capMushroom,{color:'#A5896A',round:true});reg('shiitake',capMushroom,{crack:true,color:'#9C795B'});reg('soaked_shiitake',capMushroom,{crack:true,color:'#6E5745',highlight:'#A59077',water:true});
function king(){return [p('M 34 33 L 56 32 Q 58 51 67 68 Q 76 88 44 87 Q 22 88 28 67 Q 36 46 34 33 Z',cream),p('M 52 38 L 57 38 Q 58 57 65 73 Q 66 83 55 84 L 51 82 Q 59 73 52 57 Z',shade,'none'),line('M 37 47 Q 33 60 35 75 M 44 48 L 41 77 M 49 64 L 48 79',brown,.7),ellipse(45,29,23,10,brown),p('M 24 28 Q 30 13 47 15 Q 63 15 68 28 Q 51 39 24 28 Z','#AC9579'),line('M 32 24 Q 42 18 52 21',shade,1.1)];}reg('king_oyster',king);
function cluster(s){const out=[],count=s.count||7;
 for(let j=0;j<count;j++){const x=18+j*(58/(count-1)),top=(s.short?40:23)+(j%3)*7,baseX=47+(j-(count-1)/2)*2.8;out.push(p(`M ${x-2} ${top+7} Q ${x+2} 56 ${baseX-2} 81 L ${baseX+2} 81 Q ${x+7} 51 ${x+3} ${top+7} Z`,s.stem||cream,ink,1.3));out.push(ellipse(x,top+6,s.cap||7,Math.min(s.cap||7,6),s.color||cream,ink,1.5));if(s.spots)out.push(dot(x-2,top+4,1.3,brown),dot(x+2,top+7,1.6,shade));out.push(line(`M ${x-3} ${top+5} q 2 -3 5 -2`,shade,.8));}
 out.push(p('M 35 78 Q 48 81 62 78 L 61 85 L 37 85 Z',shade,ink,1.2));return out;}
reg('enoki',cluster,{count:11,cap:4,color:cream});reg('white_beech',cluster,{count:6,cap:9,color:cream,short:true});reg('brown_beech',cluster,{count:6,cap:9,color:'#BCAB8A',spots:true,short:true});reg('tea_tree_mushroom',cluster,{count:6,cap:7,color:'#9F7A57',stem:'#C3AE8B'});
function fan(s){const out=[];const clusters=s.small?[[30,57,16],[57,59,17],[22,43,15],[49,36,18],[71,36,15]]:[[34,60,26],[60,57,26],[47,32,30]];
 for(const[x,y,r]of clusters){out.push(p(`M 47 83 Q ${x} ${y+16} ${x-r} ${y} Q ${x-r-7} ${y-17} ${x} ${y-r*.5} Q ${x+r+4} ${y-18} ${x+r} ${y} Q ${x+8} ${y+14} 47 83 Z`,s.color||'#ABAFA0'));for(let j=0;j<7;j++){const xx=x-r+4+j*(r*2-8)/6;out.push(line(`M 47 79 Q ${x} ${y+7} ${xx.toFixed(2)} ${y-2}`,s.vein||brown,.7));}out.push(line(`M ${x-r+5} ${y-4} Q ${x} ${y-16} ${x+r-5} ${y-4}`,cream,1.1));}return out;}
reg('oyster_mushroom',fan,{color:'#ADB2A6'});reg('baby_oyster',fan,{small:true,color:'#C9CEC2'});reg('white_ferula',fan,{color:cream,vein:shade});reg('maitake',fan,{small:true,color:'#9B9481'});
function frilly(s){const out=[],c=s.color||cream;
 for(const[x,y,rx,ry]of [[27,57,17,21],[64,57,19,22],[43,33,21,21],[46,60,22,22]]){
 out.push(scallop(x,y,rx,ry,s.ear?9:17,s.ear?.19:.22,c));out.push(line(`M ${x-9} ${y+8} Q ${x-15} ${y-15} ${x+5} ${y-9} Q ${x+17} ${y-6} ${x+8} ${y+10}`,s.vein||shade,1.4));
 if(!s.ear)for(let j=0;j<5;j++)out.push(line(`M ${x-12+j*5} ${y-5} q -3 -7 0 -10 M ${x-10+j*5} ${y+6} q 6 5 3 11`,s.vein||shade,.9));}
 if(s.water)out.push(...droplet());return out;}
reg('fresh_wood_ear',frilly,{ear:true,color:'#756657',vein:'#B1A38B'});reg('soaked_wood_ear',frilly,{ear:true,color:'#504C44',vein:'#A79E8B',water:true});reg('fresh_snow_fungus',frilly,{color:cream,vein:'#BCB69B'});
function shaggy(s){const out=[];
 if(s.lion){out.push(scallop(48,47,32,33,28,.06,cream));for(let j=0;j<7;j++)for(let i=0;i<8;i++){const x=22+i*7,y=28+j*7;if((x-48)**2+(y-47)**2<29**2)out.push(line(`M ${x} ${y} q 3 4 0 8`,shade,1));}return out;}
 if(s.straw){for(const[x,y,k]of [[-4,2,.9],[39,29,.64]])out.push(group([p('M 21 60 Q 21 28 46 17 Q 68 17 76 58 Q 81 83 47 85 Q 19 83 21 60 Z','#A29A85'),p('M 24 60 Q 47 71 72 57 L 76 68 Q 69 86 45 85 Q 25 84 24 60 Z',cream),line('M 31 44 Q 39 24 46 24',shade,1.5)],x,y,k));return out;}
 out.push(p('M 42 58 L 56 58 L 61 85 L 37 85 Z',cream),p('M 23 62 Q 24 29 46 15 Q 71 16 72 62 Q 48 77 23 62 Z',s.morel?'#AA9871':cream));
 if(s.morel)for(let row=0;row<6;row++)for(let col=0;col<4;col++){const x=31+col*10+(row%2)*3,y=25+row*7;if(row<2&&(col===0||col===3))continue;out.push(ellipse(x,y,3,4,'#675D47',ink,.8));}
 else for(let row=0;row<5;row++)for(let col=0;col<4;col++){const x=31+col*9,y=29+row*7;out.push(line(`M ${x} ${y} l -2 4 l 5 -1`,shade,1.1));}return out;}
reg('shaggy_mane',shaggy);reg('lions_mane',shaggy,{lion:true});reg('straw_mushroom',shaggy,{straw:true});reg('morel',shaggy,{morel:true});
