import {p,line,ellipse,dot,group,scallop,seeds,ink,cream,shade,green,light,dark,red,pink,purple,brown,yellow,orange,blue} from './drawing.mjs';
export const seafood={};const reg=(id,fn,spec={})=>seafood[id]={family:fn.name,spec,draw:()=>fn(spec)};
function fish(s){const c=s.color||blue,out=[];
 if(s.ribbon){out.push(p('M 13 30 Q 36 18 58 37 Q 80 55 73 66 Q 61 81 45 75 Q 35 71 43 64 Q 54 58 67 60 Q 59 49 47 46 L 22 44 L 13 38 Z','#AFBDB5'),p('M 23 26 Q 44 24 60 41 Q 49 32 24 35 Z',blue,'none'),line('M 33 33 Q 70 46 69 64 Q 60 74 45 69',cream,2),dot(21,32,2),line('M 25 27 Q 30 34 27 41',ink,1));for(let i=0;i<10;i++)out.push(line(`M ${33+i*3} ${30+i*2} l 1 -5`,dark,.75));return out;}
 const h=s.high?28:(s.long?16:21),x=45,y=50;
 out.push(p(`M 32 ${y-h+4} L 46 ${y-h-10} L 58 ${y-h+5} Z`,s.fin||'#728E84'));
 out.push(p(`M 37 ${y+h-4} L 56 ${y+h+9} L 64 ${y+h-6} Z`,s.fin||'#728E84'));
 out.push(p(`M 72 48 L 88 33 L 85 50 L 89 68 L 72 54 Z`,s.fin||blue));
 out.push(p(`M 11 49 Q 21 ${y-h} 43 ${y-h} Q 65 ${y-h} 76 50 Q 58 ${y+h} 39 ${y+h} Q 21 ${y+h} 11 53 Z`,c));
 out.push(p(`M 17 52 Q 40 ${y+h+1} 69 54 Q 44 ${y+h-6} 17 52 Z`,cream,'none'));
 out.push(line(`M 30 ${y-h+6} Q 41 48 30 ${y+h-6}`,dark,1.3),ellipse(22,46,3.2,3.2,cream,ink,1),dot(22,46,1.4));
 out.push(p('M 40 49 Q 53 49 56 58 Q 45 58 40 49 Z',s.fin||blue,ink,1));
 for(let row=0;row<3;row++)for(let col=0;col<5;col++){const xx=40+col*6,yy=40+row*7;if(col>3&&row!==1)continue;out.push(line(`M ${xx} ${yy} q 4 3 0 6`,dark,.6));}
 if(s.stripes)for(let i=0;i<5;i++)out.push(line(`M ${41+i*6} ${y-h+5} l -3 ${h*.6}`,dark,2.1));
 if(s.spiny)for(let i=0;i<6;i++)out.push(line(`M ${34+i*4} ${y-h+3} l 1 ${-5-i%2*4}`,dark,1.3));
 if(s.whisker)out.push(line('M 14 54 Q 6 68 18 70 M 17 55 Q 18 65 28 66',brown,1.2));
 if(s.flat)out.push(ellipse(23,55,2.2,2.2,cream,ink,.9),dot(23,55,1));
 return out;
}
for(const[id,s]of Object.entries({mackerel:{stripes:true,long:true,color:'#8FA5A4'},flounder:{high:true,flat:true,color:'#AFB39B'},grass_carp:{long:true,color:'#99ABA0'},crucian_carp:{high:true,color:'#BCC4AD'},carp:{color:'#C7B17F',whisker:true},sea_bass:{spiny:true,long:true,color:'#A4B3A9'},tilapia:{high:true,stripes:true,color:'#9CAB98'},pomfret:{high:true,color:'#C5CFBF',fin:'#92A6A0'},hairtail:{ribbon:true},yellow_croaker:{long:true,color:'#D1C383',fin:'#BAAC70'}}))reg(id,fish,s);
function fillet(s){const c=s.color||'#DC9470',out=[];out.push(p('M 15 62 Q 23 39 45 28 Q 65 23 78 48 L 82 67 Q 48 85 17 79 Z',s.skin||'#82978C'),line('M 20 66 Q 50 77 79 59',cream,1.4));
 out.push(p('M 15 62 Q 24 40 45 28 Q 65 23 78 48 Q 81 52 78 60 Q 50 78 17 73 Z',c));
 if(s.tuna){out.push(line('M 25 58 L 43 40 M 31 65 L 52 36 M 46 65 L 63 43',pink,1.1));}else if(s.white){out.push(line('M 27 57 Q 37 38 45 40 M 35 65 Q 45 48 53 46 M 47 66 Q 56 53 63 48 M 61 62 l 7 -7',shade,1));}else for(let j=0;j<5;j++)out.push(line(`M ${23+j*10} ${58-j*3} Q ${25+j*9} ${39-j} ${39+j*8} ${51-j*2} L ${35+j*8} ${67-j*3}`,cream,2));
 for(let j=0;j<9;j++)out.push(line(`M ${23+j*6} ${78-Math.floor(j/4)*4} l 1 -4`,dark,.65));return out;}
reg('salmon',fillet);reg('tuna',fillet,{color:'#B76D68',tuna:true,skin:'#AB8984'});reg('cod',fillet,{color:cream,white:true,skin:'#9BA9A1'});
function shrimp(s){const c=s.raw?'#C1C6B2':orange,out=[];
 out.push(p('M 23 32 Q 39 18 63 29 Q 88 43 79 62 Q 72 80 51 81 Q 29 80 25 64 L 33 57 Q 42 73 58 67 Q 70 60 64 48 Q 59 39 42 42 L 24 48 L 15 43 Z',c));
 out.push(p('M 28 61 L 15 61 L 12 70 L 26 70 L 18 80 L 30 83 L 36 72 Z',c));
 out.push(line('M 48 27 L 43 41 M 62 31 L 53 41 M 74 40 L 62 47 M 80 52 L 66 54 M 74 66 L 62 61 M 62 77 L 56 67 M 43 76 L 45 67',ink,1.15));
 out.push(dot(28,36,2),line('M 26 32 Q 20 15 9 18 M 25 34 Q 12 23 8 30',brown,1.3),line('M 38 43 L 34 52 L 40 58 M 47 43 L 44 53 L 50 59 M 57 45 L 54 54',brown,1));
 out.push(line('M 47 30 Q 58 29 64 36',cream,1.7));return out;}
reg('shrimp',shrimp,{raw:true});
function peeled(){const out=[];for(const[x,y,k]of [[-4,-5,.61],[39,0,.58],[14,40,.55]])out.push(group([p('M 25 30 Q 50 13 73 36 Q 83 60 58 76 Q 32 88 19 65 L 27 55 Q 37 72 53 60 Q 63 53 54 45 Q 44 38 34 45 Z',pink),line('M 42 24 L 39 39 M 57 28 L 48 40 M 68 37 L 56 46 M 72 52 L 60 52 M 63 65 L 54 58 M 47 73 L 44 64',cream,2)],x,y,k));return out;}reg('peeled_shrimp',peeled);
function crustacean(s){const out=[],c=s.color||'#A6A387';
 if(s.crab){for(let j=0;j<4;j++){out.push(line(`M 25 ${47+j*6} L ${13-j%2*3} ${50+j*7} l -3 8`,ink,3),line(`M 71 ${47+j*6} L ${83+j%2*3} ${50+j*7} l 3 8`,ink,3));}
 out.push(scallop(48,51,27,22,10,.05,c),p('M 33 45 Q 48 34 63 45 Q 56 49 48 46 Q 40 49 33 45 Z',light,'none'));
 }else{out.push(p('M 36 36 Q 48 24 59 38 L 66 75 L 58 84 L 48 81 L 40 85 L 31 74 Z',c));for(let j=0;j<5;j++)out.push(line(`M ${35-j%2*2} ${47+j*7} Q 48 ${52+j*7} ${62+j%2*2} ${47+j*7}`,ink,1.1));for(let j=0;j<4;j++)out.push(line(`M 36 ${48+j*6} l -13 4 l -3 5 M 60 ${48+j*6} l 13 4 l 3 5`,ink,1.7));}
 out.push(line('M 28 43 L 21 30 M 68 43 L 75 29',ink,5),p('M 22 32 Q 8 35 10 17 L 18 24 L 20 11 Q 34 18 22 32 Z',c),p('M 74 32 Q 88 35 86 17 L 78 24 L 76 11 Q 63 18 74 32 Z',c));out.push(dot(40,32,1.8),dot(56,32,1.8));if(!s.crab)out.push(line('M 41 34 Q 34 12 24 12 M 55 34 Q 62 12 73 13',brown,1.1));return out;}
reg('crayfish',crustacean,{color:'#A8755C'});reg('live_lobster',crustacean,{color:'#807B65'});reg('live_crab',crustacean,{crab:true});
function cephalopod(s){const out=[],c=s.color||'#D5C5B5';
 if(s.octopus){out.push(p('M 32 42 Q 22 14 46 12 Q 72 10 66 42 L 57 52 L 37 53 Z',c));for(let j=0;j<8;j++){const a=-1.5+j*3/7,x=48+Math.sin(a)*16,xx=48+Math.sin(a)*35;out.push(line(`M ${x.toFixed(2)} 46 C ${xx.toFixed(2)} 58 ${(xx+8).toFixed(2)} 87 ${(xx-3).toFixed(2)} 78 Q ${(xx-7).toFixed(2)} 73 ${(xx-1).toFixed(2)} 69`,ink,5.5),line(`M ${x.toFixed(2)} 46 C ${xx.toFixed(2)} 58 ${(xx+8).toFixed(2)} 87 ${(xx-3).toFixed(2)} 78 Q ${(xx-7).toFixed(2)} 73 ${(xx-1).toFixed(2)} 69`,c,3.5));}return out;}
 if(s.wide)out.push(p('M 23 53 Q 11 36 29 19 Q 50 8 71 24 Q 88 43 73 61 Q 52 72 23 53 Z',shade),ellipse(48,38,25,28,c));
 else out.push(p('M 46 13 L 17 42 L 33 54 L 64 51 L 81 38 Z',shade),p('M 46 13 Q 68 25 63 60 Q 48 70 34 59 Q 29 29 46 13 Z',c));
 out.push(line('M 43 23 Q 35 43 42 54',cream,1.6));for(let j=0;j<6;j++){const x=34+j*6;out.push(line(`M ${x} 59 Q ${x+(j%2?9:-9)} 77 ${x+(j%2?8:-7)} 83`,ink,3.6),line(`M ${x} 59 Q ${x+(j%2?9:-9)} 77 ${x+(j%2?8:-7)} 83`,c,2));}return out;}
reg('squid',cephalopod);reg('cuttlefish',cephalopod,{wide:true,color:'#C2C1AB'});reg('octopus',cephalopod,{octopus:true,color:'#BBA5A0'});
function shell(s){const out=[];if(s.mussel){for(const[x,y,k]of [[-3,-4,.88],[33,23,.75]])out.push(group([p('M 30 77 Q 9 61 30 35 Q 52 9 71 20 Q 86 29 65 55 Q 48 81 30 77 Z','#647873'),p('M 28 69 Q 18 58 35 39 Q 53 19 67 26 Q 70 40 53 51 Q 35 62 28 69 Z','#98A997'),line('M 30 69 Q 44 54 58 42',cream,1)],x,y,k));return out;}
 if(s.oyster){out.push(scallop(47,47,34,31,13,.13,'#B8B4A3'),scallop(47,47,27,24,12,.09,cream));out.push(p('M 29 49 Q 28 30 46 32 Q 61 29 65 44 Q 77 54 61 64 Q 36 73 29 49 Z','#ABA995'),line('M 24 60 Q 48 77 72 59 M 29 28 Q 44 18 61 27',shade,1.1));return out;}
 for(const[x,y,k]of [[-6,-7,.89],[29,31,.69]])out.push(group([p('M 16 56 Q 11 35 40 23 Q 64 19 80 41 Q 85 66 49 79 Q 19 82 16 56 Z',shade),...Array.from({length:9},(_,i)=>line(`M 41 75 Q ${15+i*8} 45 ${22+i*6} ${38-Math.sin(i/8*Math.PI)*12}`,brown,.8)),line('M 19 53 Q 38 68 75 49 M 21 44 Q 41 60 73 40',cream,1.2)],x,y,k));return out;}
reg('live_clam',shell);reg('live_mussel',shell,{mussel:true});reg('live_oyster',shell,{oyster:true});
function shellMeat(s){const out=[];
 if(s.scallop){for(const[x,y,r]of [[31,38,19],[65,60,21]])out.push(p(`M ${x-r} ${y} V ${y+13} Q ${x} ${y+26} ${x+r} ${y+13} V ${y} Z`,shade),ellipse(x,y,r,r*.58,cream),line(`M ${x-r+4} ${y+4} v 9 M ${x-r+9} ${y+7} v 8 M ${x+r-5} ${y+6} v 8`,brown,.7));return out;}
 if(s.lobster){out.push(p('M 31 28 Q 49 18 65 30 L 73 70 Q 71 85 47 84 Q 23 82 26 65 Z',cream));for(let i=0;i<5;i++)out.push(p(`M ${29-i%2*2} ${35+i*9} Q 48 ${42+i*9} ${68+i%2} ${35+i*9} l 1 4 Q 48 ${48+i*9} ${29-i%2*2} ${39+i*9} Z`,pink,ink,.8));return out;}
 for(const[x,y,k]of [[-7,-5,.7],[28,4,.68],[4,37,.66]])out.push(group([p(s.oyster?'M 20 46 Q 23 25 47 31 Q 70 20 79 44 Q 88 70 61 74 Q 44 89 25 69 Q 10 59 20 46 Z':'M 19 42 Q 39 27 55 34 Q 71 24 79 44 Q 75 71 45 73 Q 15 75 19 42 Z',s.oyster?'#A19E8D':cream),p('M 29 46 Q 49 36 67 45 Q 79 60 57 65 Q 35 73 25 57 Z',s.crab?cream:shade),line('M 29 51 Q 50 43 63 54 M 31 59 Q 45 57 59 59',s.crab?pink:brown,s.crab?2:.9)],x,y,k));return out;}
reg('crab_meat',shellMeat,{crab:true});reg('lobster',shellMeat,{lobster:true});reg('clam_meat',shellMeat);reg('oyster_meat',shellMeat,{oyster:true});reg('scallop_meat',shellMeat,{scallop:true});
